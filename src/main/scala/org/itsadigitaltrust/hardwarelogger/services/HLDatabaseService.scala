package org.itsadigitaltrust.hardwarelogger.services

import org.itsadigitaltrust.common.*
import org.itsadigitaltrust.hardwarelogger.backend.entities.*
import org.itsadigitaltrust.hardwarelogger.backend.{DataStoreLoader, HLDatabase}
import org.itsadigitaltrust.hardwarelogger.models.*
import org.itsadigitaltrust.hardwarelogger.tasks.{DatabaseTransactionTask, HLTaskRunner}

import java.sql.Timestamp
import java.time.OffsetDateTime
import java.util.concurrent.ArrayBlockingQueue.*
import java.util.concurrent.LinkedBlockingQueue
import scala.annotation.tailrec
import scala.jdk.CollectionConverters.*

private type HLEntityType[M <: HLModel] = M match
  case GeneralInfoModel | ProcessorModel => Info
  case MemoryModel => Memory
  case HardDriveModel => Disk
  case MediaModel => Media

trait HLDatabaseService:
  var itsaid: String = ""

  def connect(klazz: Class[?], dbPropsFilePath: String): Result[Unit, String]

  def findItsaIdBySerialNumber(serial: String): Option[String]
  
  def +=[M <: HLModel](model: M): Unit

  def ++=[M <: HLModel](models: Seq[M]): Unit

  def stop(): Unit

object SimpleHLDatabaseService extends HLDatabaseService, ServicesModule:
  private var db: Option[HLDatabase] = None
  private val minAmountOfTransactions = 4

  private val transactionQueue = new LinkedBlockingQueue[HLEntityCreatorWithItsaID]

  notificationCentre.subscribe(NotificationChannel.ContinueWithDuplicateDrive): (key, _) =>
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


  override def +=[M <: HLModel](model: M): Unit =
    val creator = createEC(model)
//    if !transactionQueue.contains(creator) then
    transactionQueue.add(creator)


    checkAndSave()

  override def ++=[M <: HLModel](models: Seq[M]): Unit =
    transactionQueue.addAll(models.map(createEC).asJavaCollection)
    checkAndSave(models.size)

  def findItsaIdBySerialNumber(serial: String): Option[String] =
    db match
      case Some(database) =>
        database.findItsaIdBySerialNumber(serial)
      case None =>
        None
  
  private def checkAndSave[M <: HLModel](minAmountOfTransactions: Int = minAmountOfTransactions): Unit =
    def checkForDuplicateDrives(): Iterable[String] =
      transactionQueue.asScala
        .filter(c => c.isInstanceOf[DiskCreator])
        .map(_.asInstanceOf[DiskCreator])
        .map: drive =>
          if db.isDefined then
            if db.get.doesDriveExists(drive) then
              drive.serial
            else
              ""
          else
            ""


    end checkForDuplicateDrives

    if transactionQueue.size() >= minAmountOfTransactions then
      val duplicateDrives = checkForDuplicateDrives()
      if duplicateDrives.headOption.getOrElse("") ne "" then
        sendDuplicateDriveNotification(duplicateDrives.head)
      else// if duplicateDrives.contains("") then
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
    
    HLTaskRunner("Saving to Database", generateTaskFunctions()*)(t => DatabaseTransactionTask(t)): () =>
      if transactionQueue.size() < 1 then
        sendDBSuccessNotification()



  override def stop(): Unit = ()

  private def sendDBSuccessNotification(): Unit =
    notificationCentre.publish(NotificationChannel.DBSuccess)

  private def sendDuplicateDriveNotification(serial: String): Unit =
    notificationCentre.publish(NotificationChannel.ShowDuplicateDriveWarning, serial)

  private var processor: Option[ProcessorModel] = None

  given [T]: Conversion[T, Option[T]] with
    override def apply(x: T): Option[T] = Some(x)

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
    MemoryCreator(memoryModel.size.dbString, itsaid, memoryModel.description)

  private def createHardDrive(hardDriveModel: HardDriveModel): DiskCreator =
    DiskCreator(itsaid, hardDriveModel.model, hardDriveModel.size.dbString, hardDriveModel.serial, hardDriveModel.connectionType.toString, "ATA Disk")

  private def createInfo(infoModel: GeneralInfoModel): InfoCreator =
    val totalMemory = hardwareGrabberService.memory.map(_.size.value).sum
    val processor = this.processor.getOrElse(hardwareGrabberService.processors.head)
    val creator = InfoCreator(cpuVendor = infoModel.vendor,
      itsaid = this.itsaid, cpuSerial = Some(processor.serial), totalMemory = s"$totalMemory GiB",
      cpuSpeed = processor.speed.toString, cpuDescription = processor.longDescription, cpuProduct = processor.name,
      genDesc = "TODO", genId = "itsa-hwlogger", genProduct = "CPU", genSerial = infoModel.serial, genVendor = infoModel.vendor,
      cpuWidth = processor.width.toString, os = infoModel.os, cpuCores = processor.cores.toString, insertionDate = Timestamp.from(OffsetDateTime.now().toInstant), lastUpdated = Timestamp.from(OffsetDateTime.now().toInstant))
    this.processor = None

    creator

  private def createMedia(media: MediaModel): MediaCreator =

    MediaCreator(itsaid, media.description, media.handle)