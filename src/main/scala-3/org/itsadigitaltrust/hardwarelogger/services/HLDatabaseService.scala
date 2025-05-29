package org.itsadigitaltrust.hardwarelogger.services

import org.itsadigitaltrust.common.*
import org.itsadigitaltrust.common.optional.?
import org.itsadigitaltrust.hardwarelogger.backend.HLDatabase
import org.itsadigitaltrust.hardwarelogger.backend.entities.*
import org.itsadigitaltrust.hardwarelogger.backend.types.*
import org.itsadigitaltrust.hardwarelogger.models.*
import org.itsadigitaltrust.hardwarelogger.tasks.{HLDatabaseTransactionTask, HLTaskGroupBuilder, HLTaskRunner, HardwareLoggerTask, TaskExecutor}
import org.scalafx.extras.BusyWorker
import org.scalafx.extras.BusyWorker.SimpleTask
import org.scalafx.extras.batch.{BatchRunnerWithProgress, ItemTask}
import scalafx.scene.control.{Alert, ButtonType}
import scalafx.scene.control.Alert.AlertType

import java.net.{ConnectException, Inet4Address, InterfaceAddress}
import java.sql.Timestamp
import java.time.OffsetDateTime
import java.util.concurrent.LinkedBlockingQueue
import scala.annotation.tailrec
import scala.jdk.CollectionConverters.*
import scala.reflect.{ClassTag, classTag}
import scala.util.boundary


trait HLDatabaseService:

  def connect(klazz: Class[?], dbPropsFilePath: String): Result[Unit, String]

  def connectAsync(klazz: Class[?], dbPropsFilePath: String)(finished: Result[Unit, String] => Unit): Unit = connect(klazz, dbPropsFilePath)

  def findItsaIdBySerialNumber(serial: String): Option[String]

  def replaceWithIDOrMarkAsErrorInDB(oldID: String, newID: String): Unit

  def markAllRowsWithIDInTableAsError[M <: HLModel : ClassTag](id: String): Unit

  /**
   * This does not include the wiping table.
   */
  def markAllRowsWithIDInDBAsError(itsaID: String): Unit

  def findByID[M <: HLModel : ClassTag](itsaID: String): Option[M]

  def findAllStartingWithID[M <: HLModel : ClassTag](itsaID: String): Seq[M]

  def findWipingRecord(serial: String): Option[Disk]

  def addWipingRecords(drives: HardDriveModel*): Unit

  def +=[M <: HLModel : ClassTag](model: M)(using itsaID: String = "")(using NotificationCentre[NotificationChannel], HardwareGrabberService): Unit

  def ++=[M <: HLModel : ClassTag](models: Seq[M])(using itsaID: String = "")(using NotificationCentre[NotificationChannel], HardwareGrabberService): Unit

  def stop(): Unit
end HLDatabaseService


trait CommonHLDatabase[T[_]] extends HLDatabaseService with TaskExecutor[T]:
  protected var db: Option[HLDatabase] = None
  private val minAmountOfTransactions = 4

  /**
   * Can be used to mark a new version of the program.
   */
  private final val genID = "itsa-hwlogger"

  var transactionErrorHandler: Throwable => Unit = _ => ()


  protected val transactionQueue = new LinkedBlockingQueue[HLEntityCreatorWithItsaID]
  private var noIDIndex: Option[Long] = None

  override final def connect(klazz: Class[?], dbPropsFilePath: String): Result[Unit, String] =
    Result:
      try
        HLDatabase(klazz.getResource(dbPropsFilePath).toURI.toURL) match
          case Result.Success(value) =>
            db = Option(value)
            Result.Success(())
          case Result.Error(reason) =>
            Result.error(reason.toString)
      catch case _: NullPointerException => Result.error(s"Cannot find file $dbPropsFilePath")


  override final def +=[M <: HLModel : ClassTag](model: M)(using itsaID: String = "")(using NotificationCentre[NotificationChannel], HardwareGrabberService): Unit =
    val creator = createEC(model)
    //    if !transactionQueue.contains(creator) then
    transactionQueue.add(creator)

    checkAndSave()

  override def ++=[M <: HLModel : ClassTag](models: Seq[M])(using itsaID: String = "")(using NotificationCentre[NotificationChannel], HardwareGrabberService): Unit =
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

    HLTaskRunner("Adding Wiping Records", Seq(() => db.get.addWipingRecords(disks *)) *)(HLDatabaseTransactionTask.apply)()


  given [U]: Conversion[U, Option[U]] with
    override def apply(x: U): Option[U] = Some(x)


  protected def checkAndSave[M <: HLModel : ClassTag](minAmountOfTransactions: Int = minAmountOfTransactions)(using itsaID: String = "")(using notificationCentre: NotificationCentre[NotificationChannel])(using HardwareGrabberService): Unit =
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

    def hasAnyDuplicateRows(using itsaID: String = ""): Boolean =
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


  override final def replaceWithIDOrMarkAsErrorInDB(oldID: String, newID: String): Unit =
    () //TODO: See if this needs implementing.

  override final def markAllRowsWithIDInDBAsError(itsaID: String): Unit =
    val database = db match
      case Some(value) => value
      case None => return
    val taskGroupBuilder = new HLTaskGroupBuilder(HLDatabaseTransactionTask.apply)
      .addAll(
        database.markAllRowsWithIDAsError[InfoCreator](itsaID),
        database.markAllRowsWithIDAsError[DiskCreator](itsaID),
        database.markAllRowsWithIDAsError[MemoryCreator](itsaID),
        database.markAllRowsWithIDAsError[MediaCreator](itsaID)
      )
    taskGroupBuilder.run("Marking all current rows as error.")()


  override final def markAllRowsWithIDInTableAsError[M <: HLModel : ClassTag](id: String): Unit =
    optional:
      db.?.markAllRowsWithIDAsError(id)

  def findAllStartingWithID[M <: HLModel : ClassTag](itsaID: String): Seq[M] =
    optional:
      db.?.findAllByIdStartingWith(itsaID).map(toModel)
    ?? Seq()


  def findByID[M <: HLModel : ClassTag](itsaID: String): Option[M] =
    optional:
      val result = db.?.findByID(itsaID).?
      toModel(result)


  override final def stop(): Unit = ()


  protected var processor: Option[ProcessorModel] = None


  protected def toModel[E <: ItsaEntity, M <: HLModel : ClassTag](entity: E): M =
    val model = entity match
      case info: Info if classTag[M] == classTag[ProcessorModel] =>
        ProcessorModel(
          name = info.cpuProduct.get,
          serial = info.cpuSerial ?? "",
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
          vendor = info.cpuVendor ?? "",
          serial = info.cpuSerial ?? "",
          os = info.os ?? "",
          model = info.cpuProduct ?? "",
          description = info.genDesc,
        )
      case memory: Memory =>
        MemoryModel(size = DataSize.from(memory.size) ?? DataSize(0, "MB"), description = memory.description ?? "")
      case hardDrive: Disk =>
        HardDriveModel(
          model = hardDrive.model,
          size = DataSize(0, "GiB"),
          serial = hardDrive.serial,
          description = hardDrive.description ?? "",
          health = Percentage(100),
          performance = Percentage(100),
          connectionType = HardDriveConnectionType.NVME
        )

      case media: Media =>
        MediaModel(description = media.description, handle = media.handle ?? "")
      case _ => scala.sys.error("Unknown Type!")
    model.asInstanceOf[M]

  end toModel


  protected def createEC[M <: HLModel](model: M)(using itsaID: String = "")(using hardwareGrabberService: HardwareGrabberService): HLEntityCreatorWithItsaID =
    model match
      case memory: MemoryModel => createMemory(memory, itsaID)
      case hardDriveModel: HardDriveModel => createHardDrive(hardDriveModel, itsaID)
      case infoModel: GeneralInfoModel => createInfo(infoModel, itsaID)
      case mediaModel: MediaModel => createMedia(mediaModel, itsaID)
      case processorModel: ProcessorModel =>
        processor = processorModel
        createInfo(hardwareGrabberService.generalInfo, itsaID)
      case _ => scala.sys.error("Unknown Type!")


  protected def createMemory(memoryModel: MemoryModel, itsaID: String): MemoryCreator =
    MemoryCreator(memoryModel.size.dbString, itsaID, memoryModel.description)

  protected def createHardDrive(hardDriveModel: HardDriveModel, itsaID: String): DiskCreator =
    DiskCreator(itsaID, hardDriveModel.model, hardDriveModel.size.dbString, hardDriveModel.serial, hardDriveModel.connectionType.toString, "ATA Disk")


  protected def createInfo(infoModel: GeneralInfoModel, itsaID: String)(using hardwareGrabberService: HardwareGrabberService): InfoCreator =
    val totalMemory = hardwareGrabberService.memory.map(_.size.value).sum
    val processor = this.processor.getOrElse(hardwareGrabberService.processors.head)
    val creator = InfoCreator(cpuVendor = infoModel.vendor,
      itsaID = itsaID, cpuSerial = Some(processor.serial), totalMemory = s"$totalMemory GiB",
      cpuSpeed = processor.speed.toString, cpuDescription = processor.longDescription, cpuProduct = processor.name,
      genDesc = "TODO", genId = genID, genProduct = "CPU", genSerial = infoModel.serial, genVendor = infoModel.vendor,
      cpuWidth = processor.width.toString, os = infoModel.os, cpuCores = processor.cores.toString, insertionDate = Timestamp.from(OffsetDateTime.now().toInstant), lastUpdated = Timestamp.from(OffsetDateTime.now().toInstant))
    this.processor = None
    creator
  end createInfo

  protected def createWiping(model: HardDriveModel): WipingCreator =



    WipingCreator(hddID = model.itsaID,
      serial = model.serial, model = model.model,
      insertionDate = OffsetDateTime.now, capacity = model.size.dbString,
      `type` = model.`type`, toUpdate = true, isSsd = model.`type` == "SSD",
      description = model.connectionType.toString, health = model.health.toByte, formFactor = "")


  private def createMedia(media: MediaModel, itsaID: String): MediaCreator =
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

  override final def executeTasks()(using notificationCentre: NotificationCentre[NotificationChannel])(using hardwareGrabberService: HardwareGrabberService): Unit =

    HLTaskRunner("Saving to Database", generateTaskFunctions() *)(t => HLDatabaseTransactionTask(t)): () =>
      if transactionQueue.size() < 1 then
        notificationCentre.publish(NotificationChannel.DBSuccess)

  end executeTasks

  override final def connectAsync(klazz: Class[?], dbPropsFilePath: String)(finished: Result[Unit, String] => Unit): Unit =
    def task(): Result[Unit, String] =
      connect(klazz, dbPropsFilePath)

    val simpleTask = new ItemTask[Result[Unit, String]]:
      override def name: String = "Connect to Database"

      override def run(): Result[Unit, String] =
        task()
    end simpleTask

    val worker = new BusyWorker("Connecting to Database", Seq())
    var result: Seq[BatchRunnerWithProgress.TaskResult[Result[Unit, String]]] = Seq()
    worker.doTask: () =>
      val batchRunner = new BatchRunnerWithProgress[Result[Unit, String]]("Connecting to Database", None, true):
        override def createTasks(): Seq[ItemTask[Result[Unit, String]]] = Seq(simpleTask)
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




