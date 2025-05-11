package org.itsadigitaltrust.hardwarelogger.services

import org.itsadigitaltrust.common.*
import org.itsadigitaltrust.common.optional.?
import org.itsadigitaltrust.hardwarelogger.backend.entities.*
import org.itsadigitaltrust.hardwarelogger.backend.types.*
import org.itsadigitaltrust.hardwarelogger.backend.{DataStoreLoader, HLDatabase, URLPropertyNameGetter}
import org.itsadigitaltrust.hardwarelogger.models.*
import org.itsadigitaltrust.hardwarelogger.tasks.{DatabaseTransactionTask, HLTaskGroupBuilder, HLTaskRunner, HardwareLoggerTask, TaskExecutor}
import scalafx.scene.control.{Alert, ButtonType}
import scalafx.scene.control.Alert.AlertType

import java.net.{Inet4Address, InterfaceAddress}
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

  def markAllRowsWithIDInTableAsError[M <: HLModel : ClassTag](id: String): Unit

  def markAllRowsWithIDInDBAsError(id: String): Unit

  def findByID[M <: HLModel : ClassTag](id: String = itsaID): Option[M]

  def findAllStartingWithID[M <: HLModel : ClassTag](id: String): Seq[M]

  def findWipingRecord(serial: String): Option[Disk]

  def addWipingRecords(drives: HardDriveModel*): Unit

  def +=[M <: HLModel : ClassTag](model: M)(using NotificationCentre[NotificationChannel], HardwareGrabberService): Unit

  def ++=[M <: HLModel : ClassTag](models: Seq[M])(using NotificationCentre[NotificationChannel], HardwareGrabberService): Unit

  def stop(): Unit
end HLDatabaseService

private type ModelECType[M <: HLModel] =
  M match
    case ProcessorModel | GeneralInfoModel => InfoCreator
    case MemoryModel => MemoryCreator
    case HardDriveModel => DiskCreator
    case MediaModel => MediaCreator
private type ModelEntityType[M <: HLModel] = EntityFromEC[ModelECType[M]]

private type ModelToECClassTag[M <: HLModel] =
  M match
    case GeneralInfoModel => ClassTag[InfoCreator]
    case ProcessorModel => ClassTag[InfoCreator]
    case MemoryModel => ClassTag[MemoryCreator]
    case HardDriveModel => ClassTag[DiskCreator]
    case MediaModel => ClassTag[MediaCreator]

trait CommonHLDatabase[T[_]](using ipFinderService: IPFinderService) extends HLDatabaseService with TaskExecutor[T]:
  protected var db: Option[HLDatabase] = None
  private val minAmountOfTransactions = 4

  protected val transactionQueue = new LinkedBlockingQueue[HLEntityCreatorWithItsaID]
  private var noIDIndex: Option[Long] = None

  private given urlPropertyNameGetter: URLPropertyNameGetter = props =>
    val officeAddressPrefix = props.get("db.office.url").toString.replace("jdbc:mysql://", "").split(":")(0).split("\\.", 1)(0)
    val workshopAddressPrefix = props.get("db.workshop.url").toString.replace("jdbc:mysql://", "").split(":")(0).split("\\.", 1)(0)

    ipFinderService.findAddresses().find: address =>
      val hostAddress = address.getAddress.getHostAddress
      hostAddress.startsWith(officeAddressPrefix) || hostAddress.startsWith(workshopAddressPrefix)
    match
      case Some(value) =>
        val hostAddress = value.getAddress.getHostAddress

        if hostAddress.startsWith(officeAddressPrefix) then
          Some("db.office.url")
        else if hostAddress.startsWith(workshopAddressPrefix) then
          Some("db.workshop.url")
        else
          propertyNamGetterError(Some(value))
          None

      case None =>
        propertyNamGetterError(None)
        None
  end urlPropertyNameGetter

  def propertyNamGetterError(address: Option[InterfaceAddress]): Unit

  override def connect(klazz: Class[?], dbPropsFilePath: String): Result[Unit, String] =
    def connectViaSpecificURL(connectionPropName: String): Result[Unit, String] =
      Result:
        try
          HLDatabase(klazz.getResource(dbPropsFilePath).toURI.toURL, connectionPropName) match
            case Success(value) =>
              db = Some(value)
              println("Connected to database.")
              noIDIndex = Option(value.getLatestNoIDValue)
              Result.success(())
            case Error(reason) =>
              Result.error(reason.toString)
        catch case _: NullPointerException => Result.error(s"Cannot find file $dbPropsFilePath")
    end connectViaSpecificURL
    Result:
      connectViaSpecificURL("db.office.url") match
        case Success(value) => ()
        case Error(reason) => connectViaSpecificURL("db.workshop.url") match
          case Success(value) => ()
          case Error(reason) => Result.error("Failed to connect to database!")



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
  end save

  @tailrec
  final protected def generateTaskFunctions(functions: Seq[() => Unit] = Seq()): Seq[() => Unit] =
    if transactionQueue.size() < 0 then
      return functions.getOrElse(Seq())
    val creator = transactionQueue.poll()
    if creator == null then
      functions
    else
      generateTaskFunctions(functions :+ (() => db.get.insertOrUpdate(creator)))


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
      val result = db.?.findAllByIdStartingWith(if id == "" then itsaID else id)
      result.map(toModel)
    .getOrElse(Seq())


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
          description = info.cpuDescription,
        )
      case memory: Memory =>
        MemoryModel(size = DataSize.from(memory.size), description = memory.description.getOrElse(""))
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
      case hardDrive: Wiping =>
        HardDriveModel(
          model = hardDrive.model,
          size = DataSize(0, "GiB"),
          serial = hardDrive.serial,
          description = hardDrive.description ?? "",
          health = Percentage(100),
          performance = Percentage(100),
          connectionType = HardDriveConnectionType.NVME
        )
      //        var matchingDrive = hardwareGrabberService.drives.filter(hd =>
      //          hd.model == hardDrive.model).headOption
      //        println(matchingDrive)

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
      genDesc = "TODO", genId = "itsa-hwlogger", genProduct = "CPU", genSerial = infoModel.serial, genVendor = infoModel.vendor,
      cpuWidth = processor.width.toString, os = infoModel.os, cpuCores = processor.cores.toString, insertionDate = Timestamp.from(OffsetDateTime.now().toInstant), lastUpdated = Timestamp.from(OffsetDateTime.now().toInstant))
    this.processor = None
    creator
  end createInfo

  protected def createWiping(model: HardDriveModel): WipingCreator =
    // The description is too long. So the description in the DB will only be the first sentence.
    val description = model.description.split("\\.")(0) + "."
    val newID =
      if model.itsaID.toLowerCase.startsWith("no id") then
        noIDIndex = Option((noIDIndex ?? 1L) + 1L)
        noIDIndex.get.toString
      else if model.itsaID.nonEmpty && model.itsaID.head.isLetterOrDigit then
        model.itsaID
      else
        "NO ID1"
    end newID

    WipingCreator(hddID = newID,
      serial = model.serial, model = model.model,
      insertionDate = OffsetDateTime.now, capacity = model.size.dbString,
      `type` = model.`type`, toUpdate = true, isSsd = model.`type` == "SSD",
      description = description, health = model.health.toByte, formFactor = "")


  private def createMedia(media: MediaModel): MediaCreator =
    MediaCreator(itsaID, media.description, media.handle)
end CommonHLDatabase


import IPFinderService.given

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


  override def propertyNamGetterError(address: Option[InterfaceAddress]): Unit =
    val errorMessage = address match
      case Some(value) => s"Failed to find connection for database by client's IP address: ${value.getAddress.getHostAddress}!"
      case None => "Failed find any IP addresses. Check internet connection!"
    new Alert(AlertType.Error, errorMessage, ButtonType.OK):
      title = "DB Connection Error"
      showAndWait()
end SimpleHLDatabaseService




