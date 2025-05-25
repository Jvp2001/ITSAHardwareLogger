package org.itsadigitaltrust.hardwarelogger.services

import com.mysql.cj.exceptions.CJCommunicationsException
import org.itsadigitaltrust.common.*
import org.itsadigitaltrust.common.optional.?
import org.itsadigitaltrust.hardwarelogger.backend.entities.*
import org.itsadigitaltrust.hardwarelogger.backend.types.*
import org.itsadigitaltrust.hardwarelogger.backend.{DataSourceLoader, HLDatabase, URLPropertyNameGetter}
import org.itsadigitaltrust.hardwarelogger.models.*
import org.itsadigitaltrust.hardwarelogger.tasks.{DatabaseTransactionTask, HLTaskGroupBuilder, HLTaskRunner, HardwareLoggerTask, TaskExecutor}
import scalafx.scene.control.{Alert, ButtonType}
import scalafx.scene.control.Alert.AlertType

import java.net.{ConnectException, Inet4Address, InterfaceAddress, SocketException}
import java.sql.Timestamp
import java.time.OffsetDateTime
import java.util.concurrent.LinkedBlockingQueue
import scala.annotation.tailrec
import scala.jdk.CollectionConverters.*
import scala.reflect.{ClassTag, classTag}
import scala.util.boundary


trait HLDatabaseService:
  var itsaID: String = ""

  def connect(klazz: Class[?], dbPropsFilePath: String): Result[Unit, String]

  def findItsaIdBySerialNumber(serial: String): Option[String]

  def replaceWithIDOrMarkAsErrorInDB(oldID: String, newID:String): Unit

  def markAllRowsWithIDInTableAsError[M <: HLModel : ClassTag](id: String): Unit

  /**
   * This does not include the wiping table.
   */
  def markAllRowsWithIDInDBAsError(id: String): Unit

  def findByID[M <: HLModel : ClassTag](id: String = itsaID): Option[M]

  def findAllStartingWithID[M <: HLModel : ClassTag](id: String): Seq[M]

  def findWipingRecord(serial: String): Option[Disk]

  def addWipingRecords(drives: HardDriveModel*): Unit

  def +=[M <: HLModel : ClassTag](model: M)(using NotificationCentre[NotificationChannel], HardwareGrabberService): Unit

  def ++=[M <: HLModel : ClassTag](models: Seq[M])(using NotificationCentre[NotificationChannel], HardwareGrabberService): Unit

  def stop(): Unit
end HLDatabaseService


trait CommonHLDatabase[T[_]] extends HLDatabaseService with TaskExecutor[T]:
  protected var db: Option[HLDatabase] = None
  private val minAmountOfTransactions = 4

  /**
  * Can be used to mark a new version of the program.
  */
  private final val genID = "itsa-hwlogger"

  protected val transactionQueue = new LinkedBlockingQueue[HLEntityCreatorWithItsaID]
  private var noIDIndex: Option[Long] = None

  override def connect(klazz: Class[?], dbPropsFilePath: String): Result[Unit, String] =
    def connectViaSpecificURL(connectionPropName: String): Result[Unit, String] =
      Result:
        try
          HLDatabase(klazz.getResource(dbPropsFilePath).toURI.toURL, connectionPropName) match
            case Result.Success(value) =>
              db = Some(value)
              noIDIndex = Option(value.getLatestNoIDValue)
              Result.Success(())
            case Result.Error(reason) =>
              Result.error(reason.toString)
        catch
          case _: NullPointerException => Result.error(s"Cannot find file $dbPropsFilePath")
          case _: ConnectException => Result.error(s"Cannot Connect to database! Check internet connection!")
          case _: CJCommunicationsException => Result.error(s"Cannot Connect to database! Check internet connection!")
          case _: SocketException => Result.error(s"Cannot Connect to database! Check internet connection!")
    end connectViaSpecificURL

    Result:
      connectViaSpecificURL("db.office.url") match
        case Result.Success(value) => ()
        case Result.Error(reason) => connectViaSpecificURL("db.workshop.url") match
          case Result.Success(value) => ()
          case Result.Error(reason) => Result.error("Failed to connect to database!")


  override def +=[M <: HLModel : ClassTag](model: M)(using NotificationCentre[NotificationChannel], HardwareGrabberService): Unit =
    val creator = createEC(model)
    //    if !transactionQueue.contains(creator) then
    transactionQueue.add(creator)

    checkAndSave()

  override def ++=[M <: HLModel : ClassTag](models: Seq[M])(using NotificationCentre[NotificationChannel], HardwareGrabberService): Unit =
    transactionQueue.addAll(models.map(createEC).asJavaCollection)
    checkAndSave()

  def findItsaIdBySerialNumber(serial: String): Option[String] =
    db match
      case Some(value) => value.findItsaIdBySerialNumber(serial)
      case None => None


  override def findWipingRecord(serial: String): Option[Disk] =
    optional:
      val database = db.get
      val record = database.findWipingRecord(serial)
      Disk(record.?.id, record.?.hddID, record.?.model, record.?.capacity.?, record.?.serial, record.?.`type`.?, record.?.description.?)


  def addWipingRecords(drives: HardDriveModel*): Unit =
    val disks = drives.map(createWiping)

    HLTaskRunner("Adding Wiping Records", Seq(() => db.get.addWipingRecords(disks *)) *)(DatabaseTransactionTask.apply)()


  given [U]: Conversion[U, Option[U]] with
    override def apply(x: U): Option[U] = Some(x)


  protected def checkAndSave[M <: HLModel : ClassTag](minAmountOfTransactions: Int = minAmountOfTransactions)(using notificationCentre: NotificationCentre[NotificationChannel])(using HardwareGrabberService): Unit =
    def checkForDuplicateDrives(): Iterable[String] =
      transactionQueue.asScala
        .filter(c => c.isInstanceOf[DiskCreator])
        .map(_.asInstanceOf[DiskCreator])
        .map: drive =>
          if db.get.doesDriveExists(drive) then
            drive.serial
          else
            ""
    end checkForDuplicateDrives

    def hasAnyDuplicateRows: Boolean =
      findAllStartingWithID[M](itsaID).nonEmpty


    if transactionQueue.size() >= minAmountOfTransactions then
      if hasAnyDuplicateRows then
        notificationCentre.publish(NotificationChannel.FoundDuplicateRowsWithID)
      else
        val duplicateDrives = checkForDuplicateDrives()
        if duplicateDrives.headOption.getOrElse("") ne "" then
          notificationCentre.publish(NotificationChannel.ShowDuplicateDriveWarning, duplicateDrives.head)
        else // if duplicateDrives.contains("") then
          save()

  protected def save[M <: HLModel]()(using NotificationCentre[NotificationChannel], HardwareGrabberService): Unit =
    executeTasks()

  @tailrec
  final protected def generateTaskFunctions(functions: Seq[() => Unit] = Seq()): Seq[() => Unit] =
    if transactionQueue.size() < 0 then
      return functions.getOrElse(Seq())
    val creator = transactionQueue.poll()
    if creator == null then
      functions
    else
      generateTaskFunctions(functions :+ (() => db.get.insertOrUpdate(creator)))
  end generateTaskFunctions


  given ecClassTag[M <: HLModel : ClassTag]: ClassTag[ItsaEC] =
    val result = classTag[M] match
      case ct if ct == classTag[HardDriveModel] =>
        classTag[DiskCreator]
      case ct if ct == classTag[MediaModel] =>
        classTag[MediaCreator]
      case ct if ct == classTag[MemoryModel] =>
        classTag[MemoryCreator]
      case _ =>
        classTag[InfoCreator]
    result.asInstanceOf[ClassTag[ItsaEC]]
  end ecClassTag


  override def replaceWithIDOrMarkAsErrorInDB(oldID: String, newID: String): Unit =
    () //TODO: See if this needs implementing.

  override def markAllRowsWithIDInDBAsError(id: String = itsaID): Unit =
    val database = db match
      case Some(value) => value
      case None => return
    new HLTaskGroupBuilder(DatabaseTransactionTask.apply)
      .addAll(
        database.markAllRowsWithIDAsError[InfoCreator](itsaID),
        database.markAllRowsWithIDAsError[DiskCreator](itsaID),
        database.markAllRowsWithIDAsError[MemoryCreator](itsaID),
        database.markAllRowsWithIDAsError[MediaCreator](itsaID)
      )
      .run("Marking all current rows as error.")()


  override def markAllRowsWithIDInTableAsError[M <: HLModel : ClassTag](id: String): Unit =
    optional:
      db.?.markAllRowsWithIDAsError(id)

  def findAllStartingWithID[M <: HLModel : ClassTag](id: String = itsaID): Seq[M] =
    optional:
      db.?.findAllByIdStartingWith(if id == "" then itsaID else id).map(toModel)
    ?? Seq()


  def findByID[M <: HLModel : ClassTag](id: String = itsaID): Option[M] =
    optional:
      val result = db.?.findByID(if id == "" then itsaID else id).?
      toModel(result)


  override def stop(): Unit = ()


  protected var processor: Option[ProcessorModel] = None


  protected def toModel[E <: ItsaEntity, M <: HLModel : ClassTag](entity: E): M =
    val model = entity match
      case info: Info if classTag[M] == classTag[ProcessorModel] =>
        ProcessorModel(
          name = info.cpuProduct.get,
          serial = info.cpuSerial.getOrElse(""),
          cores = info.cpuCores.map(_.toInt).getOrElse(2),
          speed = info.cpuSpeed.toLong,
          width = info.cpuWidth.map(_.toInt).getOrElse(64),
          longDescription = info.cpuDescription,
          shortDescription = ""
        )
      case info: Info =>
        GeneralInfoModel(
          computerID = info.genId,
          itsaID = info.itsaID,
          vendor = info.cpuVendor.getOrElse(""),
          serial = info.cpuSerial.getOrElse(""),
          os = info.os.getOrElse(""),
          model = info.cpuProduct.getOrElse(""),
          description = info.genDesc,
        )
      case memory: Memory =>
        MemoryModel(size = DataSize.from(memory.size) ?? DataSize(0, "GB"), description = memory.description.getOrElse(""))
      case hardDrive: Disk =>
        HardDriveModel(
          model = hardDrive.model,
          size = DataSize(0, "GiB"),
          serial = hardDrive.serial,
          description = hardDrive.description.getOrElse(""),
          health = Percentage(100),
          performance = Percentage(100),
          connectionType = HardDriveConnectionType.NVME
        )

      case media: Media =>
        MediaModel(description = media.description, handle = media.handle.getOrElse(""))
      case _ => scala.sys.error("Unknown Type!")
    model.asInstanceOf[M]

  end toModel


  protected def createEC[M <: HLModel](model: M)(using hardwareGrabberService: HardwareGrabberService): HLEntityCreatorWithItsaID =
    model match
      case memory: MemoryModel => createMemory(memory)
      case hardDriveModel: HardDriveModel => createHardDrive(hardDriveModel)
      case infoModel: GeneralInfoModel => createInfo(infoModel)
      case mediaModel: MediaModel => createMedia(mediaModel)
      case processorModel: ProcessorModel =>
        processor = processorModel
        createInfo(hardwareGrabberService.generalInfo)
      case _ => scala.sys.error("Unknown Type!")


  protected def createMemory(memoryModel: MemoryModel): MemoryCreator =
    MemoryCreator(memoryModel.size.dbString, itsaID, memoryModel.description)

  protected def createHardDrive(hardDriveModel: HardDriveModel): DiskCreator =
    DiskCreator(itsaID, hardDriveModel.model, hardDriveModel.size.dbString, hardDriveModel.serial, hardDriveModel.connectionType.toString, "ATA Disk")


  protected def createInfo(infoModel: GeneralInfoModel)(using hardwareGrabberService: HardwareGrabberService): InfoCreator =
    val totalMemory = hardwareGrabberService.memory.map(_.size.value).sum
    val processor = this.processor.getOrElse(hardwareGrabberService.processors.head)
    val creator = InfoCreator(cpuVendor = infoModel.vendor,
      itsaID = this.itsaID, cpuSerial = Some(processor.serial), totalMemory = s"$totalMemory GiB",
      cpuSpeed = processor.speed.toString, cpuDescription = processor.longDescription, cpuProduct = processor.name,
      genDesc = "TODO", genId = genID, genProduct = "CPU", genSerial = infoModel.serial, genVendor = infoModel.vendor,
      cpuWidth = processor.width.toString, os = infoModel.os, cpuCores = processor.cores.toString, insertionDate = Timestamp.from(OffsetDateTime.now().toInstant), lastUpdated = Timestamp.from(OffsetDateTime.now().toInstant))
    this.processor = None
    creator
  end createInfo

  protected def createWiping(model: HardDriveModel): WipingCreator =
    // The description is too long. So the description in the DB will only be the first sentence.
    val description = model.description.split("\\.")(0) + "."


    WipingCreator(hddID = model.itsaID,
      serial = model.serial, model = model.model,
      insertionDate = OffsetDateTime.now, capacity = model.size.dbString,
      `type` = model.`type`, toUpdate = true, isSsd = model.`type` == "SSD",
      description = description, health = model.health.toByte, formFactor = "")


  private def createMedia(media: MediaModel): MediaCreator =
    MediaCreator(itsaID, media.description, media.handle)
end CommonHLDatabase


object SimpleHLDatabaseService extends CommonHLDatabase[HardwareLoggerTask]:

  def apply()(using notificationCentre: NotificationCentre[NotificationChannel])(using HardwareGrabberService): SimpleHLDatabaseService.type =

    notificationCentre.subscribe(NotificationChannel.ContinueWithDuplicateDrive): (key, _) =>
      save()


    notificationCentre.subscribe(NotificationChannel.Reload): (key, _) =>
      transactionQueue.clear()
    this
  end apply

  override def executeTasks()(using notificationCentre: NotificationCentre[NotificationChannel])(using hardwareGrabberService: HardwareGrabberService): Unit =

    HLTaskRunner("Saving to Database", generateTaskFunctions() *)(t => DatabaseTransactionTask(t)): () =>
      if transactionQueue.size() < 1 then
        notificationCentre.publish(NotificationChannel.DBSuccess)

  end executeTasks


end SimpleHLDatabaseService




