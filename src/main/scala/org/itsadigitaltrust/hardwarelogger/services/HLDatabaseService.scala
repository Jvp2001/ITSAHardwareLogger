package org.itsadigitaltrust.hardwarelogger.services

import org.itsadigitaltrust.common
import org.itsadigitaltrust.common.Result
import org.itsadigitaltrust.hardwarelogger.backend.entities.*
import org.itsadigitaltrust.hardwarelogger.backend.{DataStoreLoader, HLDatabase}
import org.itsadigitaltrust.hardwarelogger.models.*
import ox.*

import java.sql.Timestamp
import java.time.OffsetDateTime
import java.util.concurrent.ArrayBlockingQueue


trait HLDatabaseService:
  var itsaid: String = ""

  def connect(klazz: Class[?], dbPropsFilePath: String): Result[Unit, String]

  def +=[M <: HLModel](model: M): Unit

  def stop(): Unit

object SimpleHLDatabaseService extends HLDatabaseService, ServicesModule:

  private var db: Option[HLDatabase] = None
  private val minQueueSize = 8
  private val transactionQueue = new ArrayBlockingQueue[HLEntityCreatorWithItsaID](minQueueSize)


  override def connect(klazz: Class[?], dbPropsFilePath: String): Result[Unit, String] =
    Result:
      try
        HLDatabase(klazz.getResource(dbPropsFilePath).toURI.toURL) match
          case common.Success(value) =>
            db = Some(value)
            println("Connected to database.")
            Result.success(())
          case common.Error(reason) =>
            Result.error(reason.toString)
      catch case _: NullPointerException => Result.error(s"Cannot find file $dbPropsFilePath")


  override def +=[M <: HLModel](model: M): Unit =
    val creator = createEC(model)
    transactionQueue.add(creator)


    if transactionQueue.size() >= minQueueSize then
      supervised:
        fork:
          while transactionQueue.size() > 0 do
            val c = transactionQueue.remove()
            db.get.insertOrUpdate(c)
        .join()

    if transactionQueue.size() < 1 then
      showAlertBox()
      println("Updated database.")


  override def stop(): Unit = ()

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

  private def showAlertBox(): Unit =
    notificationCentre.publish(NotificationChannel.DBSuccess)


  private def createMemory(memoryModel: MemoryModel): MemoryCreator =
    MemoryCreator(memoryModel.size.dbString, itsaid, memoryModel.description)

  private def createHardDrive(hardDriveModel: HardDriveModel): DiskCreator =
    DiskCreator(itsaid, hardDriveModel.model, hardDriveModel.size.dbString, hardDriveModel.serial, hardDriveModel.`type`.toString, "ATA Disk")

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