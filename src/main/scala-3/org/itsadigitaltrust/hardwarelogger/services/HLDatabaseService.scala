package org.itsadigitaltrust.hardwarelogger.services

import org.itsadigitaltrust.common.*
import org.itsadigitaltrust.common.collections.Dict

import org.itsadigitaltrust.hardwarelogger.backend.*
import org.itsadigitaltrust.hardwarelogger.delegates
import org.itsadigitaltrust.hardwarelogger.models.*
import org.itsadigitaltrust.hardwarelogger.tasks.*
import org.itsadigitaltrust.common.types.*

import org.itsadigitaltrust.hardwarelogger.backend.HLDatabase.Error
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{Notifiable, NotificationUserInfo, NotificationCentre, NotificationName}

import org.scalafx.extras.BusyWorker
import org.scalafx.extras.BusyWorker.SimpleTask
import org.scalafx.extras.batch.{BatchRunnerWithProgress, ItemTask}

import java.net.URI
import java.nio.file.FileSystems
import java.sql.Timestamp
import java.time.OffsetDateTime
import java.util.concurrent.LinkedBlockingQueue
import scala.annotation.tailrec
import scala.jdk.CollectionConverters.*
import scala.reflect.{ClassTag, classTag}
import scala.util.Try


trait HLDatabaseService:
  import HLDatabaseService.{given, *}
  type Error

  lazy val dbPropertiesFile: URI


  def connect(): Result[Unit, Error]

  def connectAsync()(finished: Result[Unit, Error] => Unit = _ => ()): Unit = connect()

  def findItsaIdBySerialNumber(serial: String): Option[String]

  def replaceWithIDOrMarkAsErrorInDB(oldID: String, newID: String): Unit

  def markAllRowsWithIDInTableAsError[M <: HLModel : ClassTag](id: String): Unit

  /**
   * This does not include the wiping table.
   */
  def markAllRowsWithIDInDBAsError(itsaID: String): Unit

  def findByID[M <: HLModel : ClassTag](itsaID: String): Option[M]

  def findAllStartingWithID[M <: HLModel : ClassTag](itsaID: String): Seq[Option[M]]

  def findWipingRecord(serial: String): Option[Disk]

  def addWipingRecords(drives: HardDriveModel*): Unit

  def +=[M <: HLModel : ClassTag](model: M)(using itsaID: String)(using NotificationCentre[NotificationName], HardwareGrabberService): Unit

  def ++=[M <: HLModel : ClassTag](models: Seq[M])(using itsaID: String)(using NotificationCentre[NotificationName], HardwareGrabberService): Unit

  def stop(): Unit
end HLDatabaseService


trait CommonHLDatabase[T[_]] extends HLDatabaseService with TaskExecutor[T]:
  override type Error = CommonHLDatabase.Error

  private var database: Option[HLDatabase] = None

  protected def db: Option[HLDatabase] = revalidateDB()

  private val minAmountOfTransactions = 4

  override lazy val dbPropertiesFile: URI =
    getClass.getResource("db.properties").toURI



  
  /**
   * Can be used to mark a new version of the program.
   */
  private final val genID = "itsa-hwlogger"

  var transactionErrorHandler: Throwable => Unit = _ => ()


  protected val transactionQueue = new LinkedBlockingQueue[HLEntityCreatorWithItsaID]
  private var noIDIndex: Option[Long] = None

  override final def connect(): Result[Unit, Error] =
    Result:
      HLDatabase(dbPropertiesFile) match
        case Result.Success(value) =>
          database = Option(value)
          Result.Success(())
        case Result.Error(reason) =>
          Result.error(reason)
        


  override final def +=[M <: HLModel : ClassTag](model: M)(using itsaID: String)(using NotificationCentre[NotificationName], HardwareGrabberService): Unit =
    val creator = createEC(model)
    //    if !transactionQueue.contains(creator) then
    transactionQueue.add(creator)

    checkAndSave()

  override def ++=[M <: HLModel : ClassTag](models: Seq[M])(using itsaID: String)(using NotificationCentre[NotificationName], HardwareGrabberService): Unit =
    transactionQueue.addAll(models.map(createEC).asJavaCollection)
    checkAndSave()

  def findItsaIdBySerialNumber(serial: String): Option[String] =
    db match
      case
        Some(value) => value.findItsaIdBySerialNumber(serial)
      case None => None


  override def findWipingRecord(serial: String): Option[Disk] =
      revalidateDB()
      val disk =
        val database = db.get
        val foundRecord = database.findWipingRecord(serial)
        if foundRecord.isEmpty || foundRecord == null then
          null
        else
          val record = foundRecord.get
          Disk(record.id, record.hddID, record.model, record.capacity ?? "", record.serial, record.`type` ?? "", record.description ?? "")
      end disk
      Option(disk)
  end findWipingRecord


  private def revalidateDB(): Option[HLDatabase] =
    database = database ?? HLDatabase(dbPropertiesFile).toOption
    database

  def addWipingRecords(drives: HardDriveModel*): Unit =
    val disks = drives.map(createWiping)
    revalidateDB()

    HLTaskRunner("Adding Wiping Records", Seq(() => db.get.addWipingRecords(disks *)) *)(HLDatabaseTransactionTask.apply)()


  given [U]: Conversion[U, Option[U]] with
    override def apply(x: U): Option[U] = Some(x)


  protected def checkAndSave[M <: HLModel : ClassTag](minAmountOfTransactions: Int = minAmountOfTransactions)(using itsaID: String)(using notificationCentre: NotificationCentre[NotificationName])(using HardwareGrabberService): Unit =
    def checkForDuplicateDrives(): Option[Iterable[String]] =
      transactionQueue.asScala
        .filter(c => c.isInstanceOf[DiskCreator])
        .map(_.asInstanceOf[DiskCreator])
        .map: drive =>
          if db.get.doesDriveExists(drive) then
            drive.serial
          else
            ""
        |> Option[Iterable[String]]
    end checkForDuplicateDrives

    def hasAnyDuplicateRows: Boolean =
      findAllStartingWithID[M](itsaID).nonEmpty


    if transactionQueue.size() >= minAmountOfTransactions then
      if hasAnyDuplicateRows then
        notificationCentre.post(NotificationName.FoundDuplicateRowsWithID)
      else
        val duplicateDrives = checkForDuplicateDrives().map(_.toArray) ?? Array.empty[String]
        if duplicateDrives ?? "" ne "" then
          val args = Dict:
            val drives = duplicateDrives
          .asInstanceOf[NotificationUserInfo]

          notificationCentre.post(NotificationName.ShowDuplicateDriveWarning, Option(this), args)
        else // if duplicateDrives.contains("") then
          save()

  protected def save[M <: HLModel]()(using NotificationCentre[NotificationName], HardwareGrabberService): Unit =
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


  //Q: What scala type represents this: () => Unit?
  //



  override final def replaceWithIDOrMarkAsErrorInDB(oldID: String, newID: String): Unit =
    () //TODO: See if this needs implementing.

  override final def markAllRowsWithIDInDBAsError(itsaID: String): Unit =
    val taskGroupBuilder = new HLTaskGroupBuilder(HLDatabaseTransactionTask.apply)
      .addAll(
        db.get.markAllRowsWithIDAsError[InfoCreator](itsaID),
        db.get.markAllRowsWithIDAsError[DiskCreator](itsaID),
        db.get.markAllRowsWithIDAsError[MemoryCreator](itsaID),
        db.get.markAllRowsWithIDAsError[MediaCreator](itsaID)
      )
    taskGroupBuilder.run("Marking all current rows as error.")()


  override final def markAllRowsWithIDInTableAsError[M <: HLModel : ClassTag](id: String): Unit =
    import HLDatabaseService.given
      db.get.markAllRowsWithIDAsError(id)

  def findAllStartingWithID[M <: HLModel : ClassTag](itsaID: String): Seq[Option[M]] =
    import HLDatabaseService.given
      val result = db.get.findAllByIdStartingWith(itsaID)
      val models = result.map(s => s.map(toModel)).get
      models
    ?? Seq()


  def findByID[M <: HLModel : ClassTag](itsaID: String): Option[M] =
    import HLDatabaseService.given
      val result = db.get.findByID(itsaID)
      if result.isDefined then toModel(result.get) else None


  override final def stop(): Unit = db.foreach(_.close())


  protected var processor: Option[ProcessorModel] = None


  protected def toModel[E <: ItsaEntity, M <: HLModel : ClassTag](e: E): Option[M] =

    val entity = e
    val model = entity match
      case info: Info if classTag[M] == classTag[ProcessorModel] =>
        ProcessorModel(
          name = info.cpuProduct.get,
          serial = info.cpuSerial ?? "",
          cores = info.cpuCores.map(_.toInt).getOrElse(2),
          frequency = Frequency(info.cpuSpeed.toLong, FrequencyUnit.GHz),
          width = info.cpuWidth.map(_.toInt).getOrElse(64),
          longDescription = info.cpuDescription,
          shortDescription = ""
        )
      case info: Info =>
        GeneralInfoModel(
          computerID = info.genId,
          itsaID = info.itsaID,
          vendor = info.cpuVendor ?? "",
          serial = info.cpuSerial ?? "",
          os = info.os ?? "",
          model = info.cpuProduct ?? "",
          description = info.genDesc,
        )
      case memory: Memory =>
        MemoryModel(size = DataSize.from(memory.size) ?? DataSize(), description = memory.description ?? "")
      case hardDrive: Disk =>
        HardDriveModel(
          model = hardDrive.model,
          size = DataSize(0, DataSizeUnit.GB),
          serial = hardDrive.serial,
          description = hardDrive.description ?? "",
          health = Percentage(100),
          performance = Percentage(100),
          connectionType = HardDriveConnectionType.NVME
        )
      case media: Media =>
        MediaModel(description = media.description, handle = media.handle ?? "")
      case _ => scala.sys.error("Unknown Type!")
    end model
    model.asInstanceOf[M]
  end toModel


  protected def createEC[M <: HLModel](model: M)(using itsaID: String)(using hardwareGrabberService: HardwareGrabberService): HLEntityCreatorWithItsaID =
    model match
      case memory: MemoryModel => createMemory(memory, itsaID)
      case hardDriveModel: HardDriveModel => createHardDrive(hardDriveModel, itsaID)
      case infoModel: GeneralInfoModel => createInfo(infoModel, itsaID)
      case mediaModel: MediaModel => createMedia(mediaModel, itsaID)
      case processorModel: ProcessorModel =>
        processor = processorModel
        createInfo(hardwareGrabberService.generalInfo, itsaID)
      case null => scala.sys.error("Unknown Type!")


  protected def createMemory(memoryModel: MemoryModel, itsaID: String): MemoryCreator =
    MemoryCreator(memoryModel.size.dbString, itsaID, memoryModel.description)

  protected def createHardDrive(hardDriveModel: HardDriveModel, itsaID: String): DiskCreator =
    DiskCreator(itsaID, hardDriveModel.model, hardDriveModel.size.dbString, hardDriveModel.serial, hardDriveModel.connectionType.toString, "ATA Disk")


  protected def createInfo(infoModel: GeneralInfoModel, itsaID: String)(using hardwareGrabberService: HardwareGrabberService): InfoCreator =
    val totalMemory = hardwareGrabberService.memory.map(_.size.value).sum
    val processor = hardwareGrabberService.processors.head
    val creator = InfoCreator(cpuVendor = infoModel.vendor,
      itsaID = itsaID, cpuSerial = Some(processor.serial), totalMemory = s"$totalMemory GB",
      cpuSpeed = processor.frequency.toString, cpuDescription = processor.longDescription, cpuProduct = processor.name,
      genDesc = "", genId = genID, genProduct = "CPU", genSerial = infoModel.serial, genVendor = infoModel.vendor,
      cpuWidth = processor.width.toString, os = infoModel.os, cpuCores = processor.cores.toString, insertionDate = Timestamp.from(OffsetDateTime.now().toInstant), lastUpdated = Timestamp.from(OffsetDateTime.now().toInstant))
    creator
  end createInfo

  protected def createWiping(model: HardDriveModel): WipingCreator =
    val serial = if model.serial != "?" then model.serial else ""
    WipingCreator(hddID = model.itsaID,
      serial = serial, model = model.model,
      insertionDate = OffsetDateTime.now, capacity = model.size.dbString,
      `type` = model.`type`, toUpdate = true, isSsd = model.`type` == "SSD",
      description = model.connectionType.toString, health = model.health.toByte, formFactor = "")
  end createWiping
  
  private def createMedia(media: MediaModel, itsaID: String): MediaCreator =
    MediaCreator(itsaID, media.description, media.handle)
end CommonHLDatabase

object CommonHLDatabase:
  type Error = HLDatabase.Error


class SimpleHLDatabaseService(using notificationCentre: NotificationCentre[NotificationName])(using HardwareGrabberService) extends CommonHLDatabase[HardwareLoggerTask], Notifiable[NotificationName]:

  notificationCentre.addObserver(this)

  override def onReceivedNotification(message: Message): Unit =
    message.name match
      case NotificationName.Reload => transactionQueue.clear()
      case NotificationName.ContinueWithDuplicateDrive => save()
      case NotificationName.Save => save()
      case _ => ()

  override final def executeTasks()(using notificationCentre: NotificationCentre[NotificationName])(using hardwareGrabberService: HardwareGrabberService): Unit =

    HLTaskRunner("Saving to Database", generateTaskFunctions() *)(t => HLDatabaseTransactionTask(t)): () =>
      if transactionQueue.size() < 1 then
        notificationCentre.post(NotificationName.DBSuccess)

  end executeTasks

  override final def connectAsync()(finished: Result[Unit, Error] => Unit): Unit =
    def task(): Result[Unit, Error] =
      connect()

    val simpleTask = new ItemTask[Result[Unit, Error]]:
      override def name: String = "Connect to Database"

      override def run(): Result[Unit, Error] =
        task()
    end simpleTask

    val worker = new BusyWorker("Connecting to Database", Seq())
    var result: Seq[BatchRunnerWithProgress.TaskResult[Result[Unit, Error]]] = Seq()
    worker.doTask: () =>
      val batchRunner = new BatchRunnerWithProgress[Result[Unit, Error]]("Connecting to Database", None, true):
        override def createTasks(): Seq[ItemTask[Result[Unit, Error]]] = Seq(simpleTask)
      result = batchRunner.run()
    worker.busy.onChange: (_, _, newValue) =>
      if !newValue then
        Result:
          if result.length == 1 then
            finished(Result.success(()))
          else
            finished(Result.error("Could not connect to Database!"))


  end connectAsync

end SimpleHLDatabaseService

object HLDatabaseService:
  given ecClassTag[M <: HLModel : ClassTag]: ClassTag[ItsaEC] =
    val result = classTag[M] match
      case ct if ct == classTag[HardDriveModel] =>
        if delegates.ProgramMode.mode == "HardDrive" then
          classTag[WipingCreator]
        else
          classTag[DiskCreator]
      case ct if ct == classTag[MediaModel] =>
        classTag[MediaCreator]
      case ct if ct == classTag[MemoryModel] =>
        classTag[MemoryCreator]
      case ct if ct == classTag[GeneralInfoModel] || ct == classTag[ProcessorModel] =>
        classTag[InfoCreator]
      case _ =>
        classTag[InfoCreator]
    result.asInstanceOf[ClassTag[ItsaEC]]
  end ecClassTag

end HLDatabaseService


object SimpleHLDatabaseService extends NotificationCentreModule, HardwareGrabberModule:


  var instance: Option[SimpleHLDatabaseService] = None

  def apply(): SimpleHLDatabaseService =
    instance = instance.orElse(Option(new SimpleHLDatabaseService()))
    instance.get



