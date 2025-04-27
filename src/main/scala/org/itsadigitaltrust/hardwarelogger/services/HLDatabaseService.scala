package org.itsadigitaltrust.hardwarelogger.services

import org.itsadigitaltrust.common.*
import org.itsadigitaltrust.common.optional.?
import org.itsadigitaltrust.hardwarelogger.backend.entities.*
import org.itsadigitaltrust.hardwarelogger.backend.types.*
import org.itsadigitaltrust.hardwarelogger.backend.{DataStoreLoader, HLDatabase}
import org.itsadigitaltrust.hardwarelogger.models.*
import org.itsadigitaltrust.hardwarelogger.tasks.{DatabaseTransactionTask, HLTaskRunner}

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

  def +=[M <: HLModel : ClassTag](model: M): Unit

  def ++=[M <: HLModel : ClassTag](models: Seq[M]): Unit

  def stop(): Unit

private type ModelECType[M <: HLModel] =
  M match
    case GeneralInfoModel => InfoCreator
    case ProcessorModel => InfoCreator
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


object SimpleHLDatabaseService extends HLDatabaseService, ServicesModule:


  private var db: Option[HLDatabase] = None
  private val minAmountOfTransactions = 4

  private val transactionQueue = new LinkedBlockingQueue[HLEntityCreatorWithItsaID]

  notificationCentre.subscribe(NotificationChannel.ContinueWithDuplicateDrive): (key, _) =>
    save()
  notificationCentre.subscribe(NotificationChannel.FoundDuplicateRowsWithID): (key, _) =>
    markAllRowsInDBAsError()
    save()

  notificationCentre.subscribe(NotificationChannel.Reload): (key, _) =>
    transactionQueue.clear()

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


  override def +=[M <: HLModel : ClassTag](model: M): Unit =
    val creator = createEC(model)
    //    if !transactionQueue.contains(creator) then
    transactionQueue.add(creator)

    checkAndSave()

  override def ++=[M <: HLModel : ClassTag](models: Seq[M]): Unit =
    transactionQueue.addAll(models.map(createEC).asJavaCollection)
    checkAndSave(models.size)

  def findItsaIdBySerialNumber(serial: String): Option[String] =
    db match
      case Some(database) =>
        database.findItsaIdBySerialNumber(serial)
      case None =>
        None

  given [T]: Conversion[T, Option[T]] with
    override def apply(x: T): Option[T] = Some(x)


  private def checkAndSave[M <: HLModel : ClassTag](minAmountOfTransactions: Int = minAmountOfTransactions): Unit =
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

    def hasAnyDuplicateRows: Boolean = findAllStartingWithID[M](itsaId).nonEmpty


    if transactionQueue.size() >= minAmountOfTransactions then
      if hasAnyDuplicateRows then
        notificationCentre.publish(NotificationChannel.FoundDuplicateRowsWithID)
      else
        val duplicateDrives = checkForDuplicateDrives()
        if duplicateDrives.headOption.getOrElse("") ne "" then
          sendDuplicateDriveNotification(duplicateDrives.head)
        else // if duplicateDrives.contains("") then
          save()

  private def save[M <: HLModel](): Unit =
    @tailrec
    def generateTaskFunctions(functions: Seq[() => Unit] = Seq()): Seq[() => Unit] =
      if transactionQueue.size() < 0 then
        return functions.getOrElse(Seq())
      val creator = transactionQueue.poll()
      if creator == null then
        functions
      else
        generateTaskFunctions(functions :+ (() => db.get.insertOrUpdate(creator)))

    HLTaskRunner("Saving to Database", generateTaskFunctions() *)(t => DatabaseTransactionTask(t)): () =>
      if transactionQueue.size() < 1 then
        sendDBSuccessNotification()
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
      db.?.markAllRowsWithIDAsError[ModelECType[ProcessorModel]](itsaId)

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


  private def sendDBSuccessNotification(): Unit =
    notificationCentre.publish(NotificationChannel.DBSuccess)

  private def sendDuplicateDriveNotification(serial: String): Unit =
    notificationCentre.publish(NotificationChannel.ShowDuplicateDriveWarning, serial)

  private var processor: Option[ProcessorModel] = None


  private def toModel[E <: ItsaEntity, M <: HLModel : ClassTag](entity: E): M =
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


  private def createEC[M](model: M): HLEntityCreatorWithItsaID =
    model match
      case memory: MemoryModel => createMemory(memory)
      case hardDriveModel: HardDriveModel => createHardDrive(hardDriveModel)
      case infoModel: GeneralInfoModel => createInfo(infoModel)
      case mediaModel: MediaModel => createMedia(mediaModel)
      case processorModel: ProcessorModel =>
        processor = processorModel
        createInfo(hardwareGrabberService.generalInfo)
      case _ => scala.sys.error("Unknown Type!")


  private def createMemory(memoryModel: MemoryModel): MemoryCreator =
    MemoryCreator(memoryModel.size.dbString, itsaId, memoryModel.description)

  private def createHardDrive(hardDriveModel: HardDriveModel): DiskCreator =
    DiskCreator(itsaId, hardDriveModel.model, hardDriveModel.size.dbString, hardDriveModel.serial, hardDriveModel.connectionType.toString, "ATA Disk")

  private def createInfo(infoModel: GeneralInfoModel): InfoCreator =
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


  private def createMedia(media: MediaModel): MediaCreator =
    MediaCreator(itsaId, media.description, media.handle)
end SimpleHLDatabaseService
