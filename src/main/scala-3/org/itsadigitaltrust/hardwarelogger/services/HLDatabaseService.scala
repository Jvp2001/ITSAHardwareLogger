package org.itsadigitaltrust.hardwarelogger.services

import org.itsadigitaltrust.common.*
import org.itsadigitaltrust.common.optional.?
import org.itsadigitaltrust.hardwarelogger.backend.entities.*
import org.itsadigitaltrust.hardwarelogger.backend.types.*
import org.itsadigitaltrust.hardwarelogger.backend.{DataStoreLoader, HLDatabase}
import org.itsadigitaltrust.hardwarelogger.models.*
import org.itsadigitaltrust.hardwarelogger.tasks.{DatabaseTransactionTask, HLTaskRunner, HardwareLoggerTask, TaskExecutor}

import java.sql.Timestamp
import java.time.OffsetDateTime
import java.util.concurrent.LinkedBlockingQueue
import scala.annotation.tailrec
import scala.jdk.CollectionConverters.*
import scala.reflect.{ClassTag, classTag}
import scala.util.boundary


trait HLDatabaseService:
  var itsaId: String = ""

  def connect(klazz: Class[?], dbPropsFilePath: String): Result[Unit, String]

  def findItsaIdBySerialNumber(serial: String): Option[String]

  def markAllRowsWithIDAsError[M <: HLModel : ClassTag](id: String): Unit

  def findByID[M <: HLModel : ClassTag](id: String = itsaId): Option[M]

  def findAllStartingWithID[M <: HLModel : ClassTag](id: String): Seq[M]
  def findWipingRecord(serial: String): Option[Disk]
  def +=[M <: HLModel : ClassTag](model: M)(using NotificationCentre[NotificationChannel], HardwareGrabberService): Unit

  def ++=[M <: HLModel : ClassTag](models: Seq[M])(using NotificationCentre[NotificationChannel], HardwareGrabberService): Unit

  def stop(): Unit

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

trait CommonHLDatabase[T[_]] extends HLDatabaseService with TaskExecutor[T]:
  protected var db: Option[HLDatabase] = None
  protected val minAmountOfTransactions = 4

  protected val transactionQueue = new LinkedBlockingQueue[HLEntityCreatorWithItsaID]

  override def connect(klazz: Class[?], dbPropsFilePath: String): Result[Unit, String] =
    Result:
      try
        HLDatabase(klazz.getResource(dbPropsFilePath).toURI.toURL) match
          case Success(value) =>
            db = Some(value)
            println("Connected to database.")
            Result.success(())
          case Error(reason) =>
            Result.error(reason.toString)
      catch case _: NullPointerException => Result.error(s"Cannot find file $dbPropsFilePath")


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
      db.get.findWipingRecord(serial).flatMap: wiping =>
        toModel(wiping)


  def addWipingRecords(drives: HardDriveModel*): Unit =
    val disks = drives.map(createWiping)
    val task = DatabaseTransactionTask:  () =>
      db.get.addWipingRecords(disks*)

  given[U]: Conversion[U, Option[U]] with
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
      findAllStartingWithID[M](itsaId).nonEmpty


    if transactionQueue.size() >= minAmountOfTransactions then
      if hasAnyDuplicateRows then
        notificationCentre.publish(NotificationChannel.FoundDuplicateRowsWithID)
      else
        val duplicateDrives = checkForDuplicateDrives()
        if duplicateDrives.headOption.getOrElse("") ne "" then
          notificationCentre.publish(NotificationChannel.ShowDuplicateDriveWarning, duplicateDrives.head)
        else // if duplicateDrives.contains("") then
          save()

  protected def save[M <: HLModel]()(using NotificationCentre[NotificationChannel],  HardwareGrabberService): Unit =
    executeTasks()
  end save




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


  def markAllRowsInDBAsError(): Unit =
    optional:
      db.?.markAllRowsWithIDAsError[InfoCreator](itsaId)
      db.?.markAllRowsWithIDAsError[DiskCreator](itsaId)
      db.?.markAllRowsWithIDAsError[MemoryCreator](itsaId)
      db.?.markAllRowsWithIDAsError[MediaCreator](itsaId)

  override def markAllRowsWithIDAsError[M <: HLModel : ClassTag](id: String): Unit =
    optional:
      db.?.markAllRowsWithIDAsError(id)

  def findAllStartingWithID[M <: HLModel : ClassTag](id: String = itsaId): Seq[M] =
    optional:
      val result = db.?.findAllByIdStartingWith(if id == "" then itsaId else id)
      result.map(toModel)
    .getOrElse(Seq())


  def findByID[M <: HLModel : ClassTag](id: String = itsaId): Option[M] =
    optional:
      val result = db.?.findByID(if id == "" then itsaId else id).?
      toModel(result)


  override def stop(): Unit = ()

  def convertAndClearTransactions(function: HLEntityCreator => Unit) =
    val result = transactionQueue.asScala.map: ec =>
      () => function(ec)
    .toSeq
    
    transactionQueue.clear()
    result
  end convertAndClearTransactions
    
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
      case hardDrive: Disk => HardDriveModel(
        model = hardDrive.model,
        size = DataSize(0, "GiB"),
        serial = hardDrive.serial,
        description = hardDrive.description.getOrElse(""),
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


  protected def createEC[M](model: M)(using hardwareGrabberService: HardwareGrabberService): HLEntityCreatorWithItsaID =
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
    MemoryCreator(memoryModel.size.dbString, itsaId, memoryModel.description)

  protected def createHardDrive(hardDriveModel: HardDriveModel): DiskCreator =
    DiskCreator(itsaId, hardDriveModel.model, hardDriveModel.size.dbString, hardDriveModel.serial, hardDriveModel.connectionType.toString, "ATA Disk")

  protected def createInfo(infoModel: GeneralInfoModel)(using hardwareGrabberService: HardwareGrabberService): InfoCreator =
    val totalMemory = hardwareGrabberService.memory.map(_.size.value).sum
    val processor = this.processor.getOrElse(hardwareGrabberService.processors.head)
    val creator = InfoCreator(cpuVendor = infoModel.vendor,
      itsaID = this.itsaId, cpuSerial = Some(processor.serial), totalMemory = s"$totalMemory GiB",
      cpuSpeed = processor.speed.toString, cpuDescription = processor.longDescription, cpuProduct = processor.name,
      genDesc = "TODO", genId = "itsa-hwlogger", genProduct = "CPU", genSerial = infoModel.serial, genVendor = infoModel.vendor,
      cpuWidth = processor.width.toString, os = infoModel.os, cpuCores = processor.cores.toString, insertionDate = Timestamp.from(OffsetDateTime.now().toInstant), lastUpdated = Timestamp.from(OffsetDateTime.now().toInstant))
    this.processor = None
    creator
  end createInfo

  var currentTimeStamp: Timestamp =
    Timestamp.from(OffsetDateTime.now.toInstant)
  protected def createWiping(model: HardDriveModel): WipingCreator =
    WipingCreator(hddID = model.itsaID, serial = model.serial, model = model.model, insertionDate = OffsetDateTime.now, capacity = model.size.dbString, description = model.description, health = model.health.toByte, formFactor = "")


  private def createMedia(media: MediaModel): MediaCreator =
    MediaCreator(itsaId, media.description, media.handle)
end CommonHLDatabase


object SimpleHLDatabaseService extends CommonHLDatabase[HardwareLoggerTask]:

  def apply()(using notificationCentre: NotificationCentre[NotificationChannel])(using HardwareGrabberService): SimpleHLDatabaseService.type =

    notificationCentre.subscribe(NotificationChannel.ContinueWithDuplicateDrive): (key, _) =>
      save()

    notificationCentre.subscribe(NotificationChannel.FoundDuplicateRowsWithID): (key, _) =>
      markAllRowsInDBAsError()
      save()

    notificationCentre.subscribe(NotificationChannel.Reload): (key, _) =>
      transactionQueue.clear()
    this
  end apply

  override def executeTasks()(using notificationCentre: NotificationCentre[NotificationChannel])(using hardwareGrabberService: HardwareGrabberService): Unit =

    val taskFuncs = convertAndClearTransactions(db.get.insertOrUpdate)
    transactionQueue.clear()
    HLTaskRunner("Saving to Database", taskFuncs*)(t => DatabaseTransactionTask(t)): () =>
      if transactionQueue.size() < 1 then
        notificationCentre.publish(NotificationChannel.DBSuccess)

  end executeTasks

end SimpleHLDatabaseService




