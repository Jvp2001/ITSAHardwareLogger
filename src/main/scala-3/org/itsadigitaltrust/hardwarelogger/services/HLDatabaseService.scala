package org.itsadigitaltrust.hardwarelogger.services

import org.itsadigitaltrust.common.*
import org.itsadigitaltrust.common.Operators.??
import org.itsadigitaltrust.common.collections.Dict
import org.itsadigitaltrust.common.types.*

import org.itsadigitaltrust.hardwarelogger.backend.*
import org.itsadigitaltrust.hardwarelogger.backend.HLDBErrorCode.{EntryAlreadyExists, NoEntriesFound, PCSerialNumberAlreadyExists}
import org.itsadigitaltrust.hardwarelogger.backend.HLDatabase.Error
import org.itsadigitaltrust.hardwarelogger.delegates
import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.models.*
import org.itsadigitaltrust.hardwarelogger.services.CommonHLDatabase.toDisk
import org.itsadigitaltrust.hardwarelogger.services.HLDatabaseService.ecClassTag
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.*
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.NotificationName.Reload
import org.itsadigitaltrust.hardwarelogger.tasks.*
import org.itsadigitaltrust.hardwarelogger.tasks.SimpleWaitHardwareLoggerGroupTaskGroupBuilder

import com.somainer.nameof.NameOf
import com.sun.tools.javac.util.Assert.error
import org.kohsuke.github.GitHub.connect
import org.scalafx.extras.BusyWorker
import org.scalafx.extras.BusyWorker.SimpleTask
import org.scalafx.extras.batch.{BatchRunnerWithProgress, ItemTask}
import ox.either.{fail, ok}
import sun.jvm.hotspot.runtime.PerfMemory.end

import java.io.InputStream
import java.net.URI
import java.nio.file.{FileSystems, Paths}
import java.sql.Timestamp
import java.time.OffsetDateTime
import java.util.concurrent.LinkedBlockingQueue
import scala.annotation.tailrec
import scala.jdk.CollectionConverters.*
import scala.reflect.{ClassTag, classTag}
import scala.util.{Try, Using, boundary}


trait HLDatabaseService(using HardwareLoggerTaskContext.Background) extends FrontendService:

  import HLDatabaseService.{*, given}

  type Error

  lazy val dbPropertiesFile: String

  var itsaID: String = ""

  def testConnection(): Boolean = false

  def connect()(using context: HardwareLoggerTaskContext.WaitBackground): Either[Error, HLDatabase]

  def connectAsync()(finished: Either[Error, Unit] => Unit = _ => ())(using context: HardwareLoggerTaskContext.WaitBackground): Unit = connect()

  def findItsaIdBySerialNumber(serial: String)(using context: HardwareLoggerTaskContext.WaitBackground): Either[HLDatabaseServiceException,  String]


  def replaceWithIDOrMarkAsErrorInDB(oldID: String, newID: String)(using context: HardwareLoggerTaskContext.WaitBackground): Unit

  def markAllRowsWithIDInTableAsError[M <: HLModel : ClassTag](id: String)(using context: HardwareLoggerTaskContext.WaitBackground): Either[HLDatabaseServiceException, Unit]

  def doesPCRecordExist(info: GeneralInfoModel)(using context: HardwareLoggerTaskContext.WaitBackground): Boolean

  /**
   * This does not include the wiping table.
   */
  def markAllRowsWithIDInDBAsError(itsaID: String)(using context: HardwareLoggerTaskContext.WaitBackground): Either[HLDatabaseServiceException, Unit]

  def findByID[M <: HLModel : ClassTag](itsaID: String)(using context: HardwareLoggerTaskContext.WaitBackground): Either[HLDatabaseServiceException, M]

  def findAllStartingWithID[M <: HLModel : ClassTag](itsaID: String)(using context: HardwareLoggerTaskContext.WaitBackground): Either[HLDatabaseServiceException, Vector[M]]

  def findWipingRecord(serial: String)(using context: HardwareLoggerTaskContext.WaitBackground): Either[HLDatabaseServiceException, HardDriveModel]

  def addWipingRecords(itsaID: String, drives: HardDriveModel*)(using context: HardwareLoggerTaskContext.WaitBackground): Either[HLDatabaseServiceException, Unit]

  def addPCRecord(info: GeneralInfoModel)(using context: HardwareLoggerTaskContext.WaitBackground): Either[HLDatabaseServiceException, Unit]

  def insertModel[M <: HLModel : ClassTag](model: M)(using context: HardwareLoggerTaskContext.WaitBackground): Either[HLDatabaseServiceException, Unit]
//
  def insertOrUpdateModel[M <: ItsaIDModel : ClassTag](model: M)(using context: HardwareLoggerTaskContext.WaitBackground, hardwareGrabberService: HardwareGrabberService): Either[HLDatabaseServiceException, Unit]


  def stop(): Unit
end HLDatabaseService


class HLDatabaseServiceException private(errorCode: HLDBErrorCode, message: String) extends HLFrontendException(message)

object HLDatabaseServiceException:

  def apply(errorCode: HLDBErrorCode, args: String*): HLDatabaseServiceException = errorCode match
    case HLDBErrorCode.EntryAlreadyExists => duplicateEntry(args(0), args(1))
    case HLDBErrorCode.EntryNotFound => recordNotFound(args(0), args(1))
    case HLDBErrorCode.NoEntriesFound => noEntriesFound(args(0))
    case HLDBErrorCode.PCSerialNumberAlreadyExists => duplicateEntry(args(0), args(1))
    case HLDBErrorCode.ConnectionError => connectionError((args(0) ?? "Unknown").asInstanceOf[ConnectionErrorType], args(1))

  def noEntriesFound(id: String) = new HLDatabaseServiceException(HLDBErrorCode.NoEntriesFound, s"No entries found for id: $id")

  def noEntryFound(model: String, id: String): HLDatabaseServiceException = new HLDatabaseServiceException(HLDBErrorCode.EntryNotFound, s"No entry found for model: $model and serial: $id")

  def duplicateEntriesFound(model: String): HLDatabaseServiceException = new HLDatabaseServiceException(HLDBErrorCode.EntryAlreadyExists, s"Duplicate entries found for model: $model")


  def duplicateEntry(model: String, serial: String): HLDatabaseServiceException = new HLDatabaseServiceException(HLDBErrorCode.EntryAlreadyExists, s"Duplicate entry found for model: $model and serial: $serial")

  def recordNotFound(model: String, serial: String): HLDatabaseServiceException = new HLDatabaseServiceException(HLDBErrorCode.EntryNotFound, s"Record not found for model: $model and serial: $serial")

  def connectionError(`type`: ConnectionErrorType = "Unknown", message: String = "Failed to connect to the database!"): HLDatabaseServiceException = new HLDatabaseServiceException(HLDBErrorCode.ConnectionError(`type`), message)

  def unknownError(message: String = "Unknown Error!"): HLDatabaseServiceException = new HLDatabaseServiceException(HLDBErrorCode.UnknownError, message)

  def propertiesFileError(message: String = "Failed to load propertiesFilePath file!"): HLDatabaseServiceException = new HLDatabaseServiceException(HLDBErrorCode.PropertiesError, message)
end HLDatabaseServiceException

trait CommonHLDatabase(using HardwareLoggerTaskContext.WaitBackground) extends HLDatabaseService:
  override type Error = HLDatabaseServiceException


  type HLDatabaseResultError[S <: HLModel] = Either[HLDatabaseServiceException, Option[S]]

  private lazy val database: Either[HLDatabaseServiceException, HLDatabase] = connect()

  protected def db: Either[HLDatabaseServiceException, HLDatabase] = database //revalidateDB()

  override lazy val dbPropertiesFile: String =
    (getClass.getResource("db.propertiesFilePath").toURI |> Paths.get).toFile.getAbsolutePath


  override def testConnection(): Boolean =
    db.map(_.testConnection()).getOrElse(false)


  /**
   * Can be used to mark a new version of the program.
   */
  private final val genID = "itsa-hwlogger"

  var transactionErrorHandler: Throwable => Unit = _ => ()

  protected val transactionQueue = new LinkedBlockingQueue[HLEntityCreatorWithItsaID]
  private var noIDIndex: Option[Long] = None


  override def connect()(using context: HardwareLoggerTaskContext.WaitBackground): Either[HLDatabaseServiceException, HLDatabase] =
    import HLDatabaseService.given
    ox.either:
      val dbValue = HLDatabase(dbPropertiesFile)
      dbValue match
        case Left(error) => error match
          case Error.ConnectionError => HLDatabaseServiceException.connectionError().fail()
          case _ => HLDatabaseServiceException.unknownError("An unknown error has occurred when connecting to the database.").fail()
        case Right(value) => value
  end connect


  def findItsaIdBySerialNumber(serial: String)(using context: HardwareLoggerTaskContext.WaitBackground): Either[HLDatabaseServiceException, String] =
    import CommonHLDatabase.given
    db.flatMap: dbValue =>
      dbValue
        .findItsaIdByGenSerialNumber(serial)
        .left.map:
          case HLDBErrorCode.ConnectionError => HLDatabaseServiceException.connectionError()
          case HLDBErrorCode.EntryNotFound => HLDatabaseServiceException.recordNotFound("Wiping Record", serial)
          case _ => HLDatabaseServiceException.unknownError("Unknown error has occurred when fetching wiping record from DB.")


  override def findWipingRecord(serial: String)(using context: HardwareLoggerTaskContext.WaitBackground): Either[HLDatabaseServiceException, HardDriveModel] =
    db.flatMap: dbValue =>
      dbValue
        .findWipingRecord(serial) // Either[HLDBErrorCode, Option[Wiping]]
        .left.map:
          case HLDBErrorCode.ConnectionError => HLDatabaseServiceException.connectionError()
          case HLDBErrorCode.EntryNotFound => HLDatabaseServiceException.recordNotFound("Wiping Record", serial)
          case _ => HLDatabaseServiceException.unknownError("Unknown error has occurred when fetching wiping record from DB.")
        .flatMap(_.toRight(HLDatabaseServiceException.recordNotFound("Wiping Record", serial)))
        .flatMap(w => toModel[Wiping, HardDriveModel](w).toRight(
          HLDatabaseServiceException.unknownError("Found wiping record, but could not map it to HardDriveModel.")
        ))
  end findWipingRecord


  override def doesPCRecordExist(info: GeneralInfoModel)(using context: HardwareLoggerTaskContext.WaitBackground): Boolean =
    findItsaIdBySerialNumber(info.serial).isRight ||
      info.itsaID.map: id =>
        findByID(id).isRight
      ?? false

  import scala.reflect.Selectable.reflectiveSelectable

  override def insertOrUpdateModel[M <: ItsaIDModel : ClassTag](model: M)(using context: HardwareLoggerTaskContext.WaitBackground, hardwareGrabberService: HardwareGrabberService): Either[HLDatabaseServiceException, Unit] =
    ox.either:
      val dbValue: HLDatabase = database.ok()
      val modelClassTag = classTag[M]

      dbValue.insertOrUpdate(createEC(model)) match
        case Left(error) => error match
          case HLDBErrorCode.EntryAlreadyExists => HLDatabaseServiceException.duplicateEntry(NameOf.nameOf[M].toString(), model.serial).fail()
          case _ => HLDatabaseServiceException.unknownError("An unknown error has occurred when inserting or updating record to DB.").fail()
        case Right(_) => ()
  end insertOrUpdateModel
  override def insertModel[M <: HLModel : ClassTag](model: M)(using context: HardwareLoggerTaskContext.WaitBackground): Either[HLDatabaseServiceException, Unit] =
    model match
      case info: GeneralInfoModel => addPCRecord(info)
      case _ => addWipingRecords(genID, model.asInstanceOf[HardDriveModel])
  override def addPCRecord(info: GeneralInfoModel)(using context: HardwareLoggerTaskContext.WaitBackground): Either[HLDatabaseServiceException, Unit] =
    ox.either:
      if doesPCRecordExist(info) then
        HLDatabaseServiceException.duplicateEntry(NameOf.nameOf[GeneralInfoModel].toString(), info.serial).fail()
      else
        val dbValue: HLDatabase = db.ok()
        db


  def addWipingRecords(itsaID: String, drives: HardDriveModel*)(using context: HardwareLoggerTaskContext.WaitBackground): Either[HLDatabaseServiceException, Unit] =
    val updatedDrives = drives.map: drive =>
      drive.copy(itsaID = drive.itsaID ?? itsaID)

    def addRecords(): Either[HLDatabaseServiceException, Unit] =
      ox.either:
        val dbValue: HLDatabase = db.ok()
        dbValue.addWipingRecords(drives.map(toWiping) *).left.map:
          case HLDBErrorCode.ConnectionError => HLDatabaseServiceException.connectionError()
          case HLDBErrorCode.EntryAlreadyExists => HLDatabaseServiceException.duplicateEntriesFound("Wiping Record")
          case _ => HLDatabaseServiceException.unknownError("Unknown error has occurred when adding wiping records to DB.")


    val records = updatedDrives.map(toWiping)
    ox.either:
      val dbValue: HLDatabase = db.ok()


  given [U]: Conversion[U, Option[U]] with
    override def apply(x: U): Option[U] = Some(x)


  override def markAllRowsWithIDInTableAsError[M <: HLModel : ClassTag](id: String)(using context: HardwareLoggerTaskContext.WaitBackground): Either[HLDatabaseServiceException, Unit] = ???



  override final def replaceWithIDOrMarkAsErrorInDB(oldID: String, newID: String)(using context: HardwareLoggerTaskContext.WaitBackground): Unit =
    () //TODO: See if this needs implementing.

  override final def markAllRowsWithIDInDBAsError(itsaID: String)(using context: HardwareLoggerTaskContext.WaitBackground): Either[HLDatabaseServiceException, Unit] =
    ox.either:
      val theDB: HLDatabase = db.ok()
      val taskGroupBuilder = new SimpleWaitHardwareLoggerGroupTaskGroupBuilder[Unit]()

      taskGroupBuilder.addAll("Tasks")(
        theDB.markAllRowsWithIDAsError[InfoCreator, Info](itsaID),
        theDB.markAllRowsWithIDAsError[DiskCreator, Disk](itsaID),
        theDB.markAllRowsWithIDAsError[MemoryCreator, Memory](itsaID),
        theDB.markAllRowsWithIDAsError[MediaCreator,Media](itsaID)
      )(using HardwareLoggerTaskContext.waitBackground)

      taskGroupBuilder.run("Marking all current rows as error.")()
  end markAllRowsWithIDInDBAsError

  type HLEntityFromModel[M <: HLModel] =
    M match
      case HardDriveModel => Disk
      case MemoryModel => Memory
      case GeneralInfoModel => Info
      case ProcessorModel => Info
      case MediaModel => Media
      case HLModel => HLEntity
  type HLECFromModel[M <: HLModel] =
    M match
      case HardDriveModel => DiskCreator
      case MemoryModel => MemoryCreator
      case GeneralInfoModel => InfoCreator
      case ProcessorModel => InfoCreator
      case MediaModel => MediaCreator
      case HLModel => HLEntityCreatorWithItsaID

  type HLEntityClassTagFromModel[M <: HLModel] =
    M match
      case HardDriveModel => ClassTag[Disk]
      case MemoryModel => ClassTag[MemoryModel]
      case GeneralInfoModel | ProcessorModel => ClassTag[Info]
      case MediaModel => ClassTag[Media]
      case HardDriveModel => ClassTag[Wiping]
      case HLModel => ClassTag[HLEntity]


  type HLTableInfoFromModel[M <: HLModel] = HLTableInfo[HLECFromModel[M], HLEntityFromModel[M]]

  private def fetch[M <: HLModel : ClassTag, E <: EntityFromEC[EC] : ClassTag, EC <: ItsaEC : ClassTag](itsaID: String): Either[HLDatabaseServiceException, Vector[EntityFromEC[EC]]] =
    val result = db.flatMap: dbValue =>
      dbValue.findAllByIdStartingWith[E, EC](itsaID).left.flatMap:
        case HLDBErrorCode.ConnectionError => Left(HLDatabaseServiceException.connectionError())
        case HLDBErrorCode.EntryNotFound => Left(HLDatabaseServiceException.noEntryFound(classTag[M].toString, itsaID))
        case HLDBErrorCode.UnknownError => Left(HLDatabaseServiceException.unknownError())
        case HLDBErrorCode.NoEntriesFound => Left(HLDatabaseServiceException.noEntriesFound(itsaID))
        case HLDBErrorCode.EntryAlreadyExists => Left(HLDatabaseServiceException.duplicateEntriesFound(classTag[M].toString))
        case _ => Left(HLDatabaseServiceException.unknownError("Unknown error has occurred when fetching records from DB."))
    result

  override def findAllStartingWithID[M <: HLModel : ClassTag](itsaID: String)(using context: HardwareLoggerTaskContext.WaitBackground): Either[HLDatabaseServiceException, Vector[M]] =
    ox.either:
      val dbValue: HLDatabase = db.ok()

      def mapDbError(code: HLDBErrorCode): HLDatabaseServiceException = code match
        case HLDBErrorCode.ConnectionError => HLDatabaseServiceException.connectionError()
        case HLDBErrorCode.EntryNotFound => HLDatabaseServiceException.noEntryFound(classTag[M].toString, itsaID)
        case HLDBErrorCode.NoEntriesFound => HLDatabaseServiceException.noEntriesFound(itsaID)
        case _ => HLDatabaseServiceException.unknownError("Unknown error has occurred when fetching records from DB.")

      val result: Vector[M] = classTag[M] match
        case ct if ct == classTag[HardDriveModel] =>
          dbValue
            .findAllByIdStartingWith[Disk, DiskCreator](itsaID)
            .left.map(mapDbError)
            .flatMap: entities =>
              Right(entities.flatMap(disk => toModel[Disk, M](disk)))
            .ok()

        case ct if ct == classTag[MemoryModel] =>
          dbValue
            .findAllByIdStartingWith[Memory, MemoryCreator](itsaID)
            .left.map(mapDbError)
            .flatMap: entities =>
              Right(entities.flatMap(mem => toModel[Memory, M](mem)))
            .ok()

        case ct if ct == classTag[MediaModel] =>
          dbValue
            .findAllByIdStartingWith[Media, MediaCreator](itsaID)
            .left.map(mapDbError)
            .flatMap: entities =>
              Right(entities.flatMap(media => toModel[Media, M](media)))
            .ok()

        case ct if ct == classTag[GeneralInfoModel] =>
          dbValue
            .findAllByIdStartingWith[Info, InfoCreator](itsaID)
            .left.map(mapDbError)
            .flatMap: entities =>
              Right(entities.flatMap(info => toModel[Info, M](info)))
            .ok()

        case ct if ct == classTag[ProcessorModel] =>
          dbValue
            .findAllByIdStartingWith[Info, InfoCreator](itsaID)
            .left.map(mapDbError)
            .flatMap: entities =>
              Right(entities.flatMap(info => toModel[Info, M](info)))
            .ok()

        case _ =>
          Left(HLDatabaseServiceException.noEntryFound(classTag[M].toString, itsaID)).ok()

      result

  end findAllStartingWithID

  def findByID[M <: HLModel : ClassTag](itsaID: String)(using context: HardwareLoggerTaskContext.WaitBackground): Either[HLDatabaseServiceException, M] =
    import HLDatabaseService.given
    def noEntriesFound = HLDatabaseServiceException.apply(HLDBErrorCode.NoEntriesFound, classTag[M].toString, itsaID)

    ox.either:
      val dbValue: HLDatabase = db.ok()

      def mapDbError(code: HLDBErrorCode): HLDatabaseServiceException = code match
        case HLDBErrorCode.ConnectionError => HLDatabaseServiceException.connectionError()
        case HLDBErrorCode.EntryNotFound => HLDatabaseServiceException.noEntryFound(classTag[M].toString, itsaID)
        case HLDBErrorCode.NoEntriesFound => HLDatabaseServiceException.noEntriesFound(itsaID)
        case _ => HLDatabaseServiceException.unknownError("Unknown error has occurred when fetching record from DB.")

      val out: Either[HLDatabaseServiceException, M] =
        classTag[M] match
          case ct if ct == classTag[HardDriveModel] =>
            dbValue
              .findByID[WipingCreator, Wiping](itsaID)
              .left.map(mapDbError)
              .flatMap:
                case wiping =>
                  toModel[Wiping, HardDriveModel](wiping) match
                    case Some(hd) => Right(hd.asInstanceOf[M])
                    case None => Left(noEntriesFound)
                case _ =>
                  Left(noEntriesFound)


          case _ =>
            Left(noEntriesFound)

      out.ok()


  end findByID


  override final def stop(): Unit = db.foreach(_.close())


  protected var processor: Option[ProcessorModel] = None

  private def createHardDriveModel[M <: HLModel : ClassTag, E <: HLEntity](hardDrive: Wiping) =
    HardDriveModel(
      model = hardDrive.model,
      size = DataSize(0, DataSizeUnit.GB),
      serial = hardDrive.serial,
      description = hardDrive.description ?? "",
      health = Percentage(100),
      performance = Percentage(100),
      connectionType = HardDriveConnectionType.NVME
    )

  private def createHardDriveModel[M <: HLModel : ClassTag, E <: HLEntity](hardDrive: Disk) =

    HardDriveModel(
      model = hardDrive.model,
      size = DataSize(0, DataSizeUnit.GB),
      serial = hardDrive.serial,
      description = hardDrive.description ?? "",
      health = Percentage(100),
      performance = Percentage(100),
      connectionType = HardDriveConnectionType.NVME
    )

  /**
   * Maps a DB entity into a frontend model.
   *
   * Notes:
   *  - Call sites currently use it like: toModel[Wiping, HardDriveModel](wiping)
   *  - We gate casts via both runtime pattern matching on the entity and ClassTag on M
   *  - Returns None if the mapping is unknown / mismatched.
   */
  private def toModel[E <: HLEntity, M <: HLModel : ClassTag](entity: E): Option[M] =
    val mCt = classTag[M]

    (mCt, entity) match
      // Wiping row -> HardDriveModel (used by findWipingRecord / findByID)
      case (ct, wiping: Wiping) if ct == classTag[HardDriveModel] =>
        Some(
          createHardDriveModel[HardDriveModel, HLEntity](wiping)
            .asInstanceOf[M]
        )

      // Disk row -> HardDriveModel (useful if you later query diskTable in normal mode)
      case (ct, disk: Disk) if ct == classTag[HardDriveModel] =>
        Some(
          createHardDriveModel[HardDriveModel, HLEntity](disk.asInstanceOf[HLEntity & Disk])
            .asInstanceOf[M]
        )

      case _ =>
        None

  private def createInfoModel[M <: HLModel : ClassTag, E <: HLEntity](info: E & Info) =
  {
    GeneralInfoModel(
      computerID = info.genId,
      itsaID = info.itsaID,
      vendor = info.cpuVendor ?? "",
      serial = info.cpuSerial ?? "",
      os = info.os ?? "",
      model = info.cpuProduct ?? "",
      description = info.genDesc,
    )
  }

  private def createProcessorModel[M <: HLModel : ClassTag, E <: HLEntity](info: E & Info) =
  {
    ProcessorModel(
      name = info.cpuProduct.get,
      serial = info.cpuSerial ?? "",
      cores = info.cpuCores.map(_.toInt) ?? 2,
      frequency = Frequency(info.cpuSpeed.toLong, FrequencyUnit.GHz),
      width = info.cpuWidth.map(_.toInt) ?? 64,
      longDescription = info.cpuDescription,
      shortDescription = ""
    )
  }

  protected transparent inline def createEC[M <: HLModel](model: M)(using hardwareGrabberService: HardwareGrabberService): Any =

    model match
      case memory: MemoryModel => toMemory(memory)
      case hardDriveModel: HardDriveModel =>
        if ProgramMode.isInNormalMode then
          toHardDrive(hardDriveModel)
        else
          toWiping(hardDriveModel)
      case infoModel: GeneralInfoModel => toInfo(infoModel)
      case mediaModel: MediaModel => toMedia(mediaModel)
      case processorModel: ProcessorModel =>
        processor = processorModel
        val info: GeneralInfoModel = hardwareGrabberService.generalInfo
        toInfo(info)
      case null => scala.sys.error("Unknown Type!")


  protected def toMemory(memoryModel: MemoryModel): MemoryCreator =
    MemoryCreator(memoryModel.size.dbString, itsaID, memoryModel.description)

  protected def toHardDrive(hardDriveModel: HardDriveModel): DiskCreator =
    DiskCreator(itsaID, hardDriveModel.model, hardDriveModel.size.dbString, hardDriveModel.serial, hardDriveModel.connectionType.toString, "ATA Disk")


  protected def toInfo(infoModel: GeneralInfoModel)(using hardwareGrabberService: HardwareGrabberService): InfoCreator =
    val totalMemory = hardwareGrabberService.memory.map(_.size.value).sum
    val processor = hardwareGrabberService.processors.head
    val creator = InfoCreator(cpuVendor = infoModel.vendor,
      itsaID = itsaID, cpuSerial = Some(processor.serial), totalMemory = s"$totalMemory GB",
      cpuSpeed = processor.frequency.toString, cpuDescription = processor.longDescription, cpuProduct = processor.name,
      genDesc = "", genId = genID, genProduct = "CPU", genSerial = infoModel.serial, genVendor = infoModel.vendor,
      cpuWidth = processor.width.toString, os = infoModel.os, cpuCores = processor.cores.toString, insertionDate = Timestamp.from(OffsetDateTime.now().toInstant), lastUpdated = Timestamp.from(OffsetDateTime.now().toInstant))
    creator
  end toInfo

  protected def toWiping(model: HardDriveModel): WipingCreator =
    val serial = if model.serial != "?" then model.serial else ""
    WipingCreator(hddID = model.itsaID,
      serial = serial, model = model.model,
      insertionDate = OffsetDateTime.now, capacity = model.size.dbString,
      `type` = model.`type`, toUpdate = true, isSsd = model.`type` == "SSD",
      description = model.connectionType.toString, health = model.health.toByte, formFactor = "")
  end toWiping

  private def toMedia(media: MediaModel): MediaCreator =
    MediaCreator(itsaID, media.description, media.handle)
end CommonHLDatabase

object CommonHLDatabase:
  type Error = HLDatabase.Error

  extension (wiping: Wiping)
    def toDisk: Disk =
      Disk(wiping.id, wiping.hddID, wiping.model, wiping.capacity ?? "", wiping.serial, wiping.`type` ?? "", wiping.description ?? "")

end CommonHLDatabase


object HLDatabaseService:
  type ModelToEntity[M <: HLModel] = M match
    case MemoryModel => Memory
    case HardDriveModel => Disk
    case GeneralInfoModel => Info
    case MediaModel => Media
    case ProcessorModel => Info
  type ModelToEC[M <: HLModel] = ECFromEntity[ModelToEntity[M]]
  type ModelToTableInfo[M <: HLModel] = HLTableInfo[ModelToEC[M], ModelToEntity[M]]

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


  given modelToTable[M <: HLModel : ClassTag]: ModelToTableInfo[M] =
    val result = classTag[M] match
      case ct if ct == classTag[HardDriveModel] =>
        if delegates.ProgramMode.mode == "HardDrive" then
          tables.wipingTable
        else
          tables.diskTable
      case ct if ct == classTag[MediaModel] =>
        tables.mediaTable
      case ct if ct == classTag[MemoryModel] =>
        tables.memoryTable
      case ct if ct == classTag[GeneralInfoModel] || ct == classTag[ProcessorModel] =>
        tables.infoTable
      case _ =>
        tables.infoTable
    result.asInstanceOf[ModelToTableInfo[M]]
end HLDatabaseService


object MySQLHLDatabaseService extends NotificationCentreModule, HardwareGrabberModule
