
package org.itsadigitaltrust.hardwarelogger.services

import com.augustnagro.magnum
import com.augustnagro.magnum.{DbCodec, DbCon, DbTx}
import com.mysql.cj.jdbc.MysqlDataSource
import com.mysql.cj.jdbc.exceptions.{MySQLQueryInterruptedException, MySQLTimeoutException}
import com.somainer.nameof.NameOf
import org.itsadigitaltrust.common.Operators.{??, toOption, |>}
import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.common.optional
import org.itsadigitaltrust.common.optional.!
import org.itsadigitaltrust.common.types.DataSizeType.numericDataSize.mkNumericOps
import org.itsadigitaltrust.common.types.DataSizeType.{DataSize, DataSizeUnit}
import org.itsadigitaltrust.common.types.FrequencyType.Frequency
import org.itsadigitaltrust.common.types.PercentageType.Percentage
import org.itsadigitaltrust.hardwarelogger.backend.{ConnectionErrorType, DataSourceLoaderService, HLDBErrorCode, HLSqlDataSource, HLTableInfo, repos}
import org.itsadigitaltrust.hardwarelogger.backend.entities.*
import org.itsadigitaltrust.hardwarelogger.backend.repos.HLRepoWithID
import org.itsadigitaltrust.hardwarelogger.backend.types.{ECFromEntity, EntityFromEC}
import org.itsadigitaltrust.hardwarelogger.backend.utils.IPAddressFinder
import org.itsadigitaltrust.hardwarelogger.core.BuildInfo
import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.models.*
import ox.either.{fail, ok}

import java.sql.Timestamp
import java.time.OffsetDateTime
import scala.Conversion.into
import scala.compiletime.deferred
import scala.reflect.{ClassTag, classTag}
import scala.util.boundary.Label

trait HLDatabaseService(using localhost: Boolean, dataSourceLoaderService: DataSourceLoaderService[HLSqlDataSource]) extends FrontendService:

  type Error <: Throwable

  val useLocalhost: Boolean = localhost


  var itsaID: String = ""

  def testConnection(): Boolean = false

  def connect(): Either[Error, Unit]


  def findItsaIdBySerialNumber(serial: String): Either[HLDatabaseServiceException, String]


  def replaceWithIDOrMarkAsErrorInDB(oldID: String, newID: String): Unit

  def markAllRowsWithIDInTableAsError[M <: HLModel : ClassTag](id: String): Either[HLDatabaseServiceException, Unit]

  def doesPCRecordExist(info: GeneralInfoModel)(using hardwareGrabberService: HardwareGrabberService): Boolean

  /**
   * This does not include the wiping table.
   */
  def markAllRowsWithIDInDBAsError(itsaID: String): Either[HLDatabaseServiceException, Unit]

  def findByID[M <: HLModel : ClassTag](itsaID: String)(using hardwareGrabberService: HardwareGrabberService): Either[HLDatabaseServiceException, M]

  def findDriveBySerialNumber(serial: String): Either[HLDatabaseServiceException, HardDriveModel]

  def findWipingRecord(serial: String): Either[HLDatabaseServiceException, HardDriveModel]

  def addWipingRecords(itsaID: String, drives: HardDriveModel*): Either[HLDatabaseServiceException, Unit]

  def addPCRecord(info: GeneralInfoModel)(using hardwareGrabberService: HardwareGrabberService): Either[HLDatabaseServiceException, Unit]

  def insertModel[M <: HLModel : ClassTag](model: M)(using hardwareGrabberService: HardwareGrabberService): Either[HLDatabaseServiceException, Unit]

  //
  def insertOrUpdateModel[M <: ItsaIDModel : ClassTag](model: M)(using hardwareGrabberService: HardwareGrabberService): Either[HLDatabaseServiceException, Unit]


  def findWipingRecordByID(itsaID: String): Either[HLDatabaseServiceException, HardDriveModel]

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

class MySQLHardwareLoggerDatabaseService(using localhost: Boolean = false, dataSourceLoaderService: DataSourceLoaderService[HLSqlDataSource]) extends HLDatabaseService with HWLLoggable:
  override type Error = HLDatabaseServiceException



  /**
   * Can be used to mark a new version of the program.
   */
  private final val genID = "itsa-hwlogger"

  var transactionErrorHandler: Throwable => Unit = _ => ()


  private var noIDIndex: Option[Long] = None

  type HLDBTransaction[T] = Either[HLDatabaseServiceException, T]

  /**
   * Combined transaction body function that has access to both DbTx (magnum context)
   * and the ox.either error handling context via Label.
   */
  type CombinedTransactionBody[T] = DbTx ?=> Label[HLDBTransaction[T]] ?=> HLDBTransaction[T]

  /**
   * Executes a database transaction with both magnum.transact and ox.either contexts.
   * The body function receives DbTx as a given and can use ox.either's fail/ok operations.
   */
  def transact[T](f: DbTx ?=> HLDBTransaction[T]): HLDBTransaction[T] =
    ox.either.catchAll:
      val ds: HLSqlDataSource = dataSource.ok()
        magnum.transact(ds)(f) match
          case Left(error) => error.fail()
          case Right(result) => result


    .left.map(e => HLDatabaseServiceException.connectionError("Unknown", e.getMessage))


  val dataSource: Either[HLDatabaseServiceException, HLSqlDataSource] =  dataSourceLoaderService.dataSource.left.map(HLDatabaseServiceException(_))

  end dataSource

  override def connect(): Either[HLDatabaseServiceException, Unit] =
    ox.either:
      try
        val ds: MysqlDataSource = dataSource.ok()
        magnum.connect(ds):
          ()
      catch
        case e: MySQLTimeoutException =>
          HLDatabaseServiceException.connectionError("Unknown", e.getMessage).fail()
        case e: MySQLQueryInterruptedException =>
          HLDatabaseServiceException.connectionError("Unknown", e.getMessage).fail()

  end connect

  override def findItsaIdBySerialNumber(serial: String): Either[HLDatabaseServiceException, String] =
    transact[String]:
      repos.infoRepo.findItsaIdBySerialNumber(serial).toRight(HLDatabaseServiceException.recordNotFound("General Info", serial))


  override def findWipingRecord(serial: String): Either[HLDatabaseServiceException, HardDriveModel] =
    transact:
      repos.wipingRepo.findWipingRecord(serial).map(createHardDriveModel)
        .toRight(HLDatabaseServiceException.noEntryFound("Wiping Record", serial))



  end findWipingRecord


  override def doesPCRecordExist(info: GeneralInfoModel)(using hardwareGrabberService: HardwareGrabberService): Boolean =
    findItsaIdBySerialNumber(info.serial).isRight ||
      info.itsaID.map: id =>
        findByID(id).isRight
      ?? false


  override def insertOrUpdateModel[M <: ItsaIDModel : ClassTag](model: M)(using hardwareGrabberService: HardwareGrabberService): Either[HLDatabaseServiceException, Unit] =
    transact:
      ox.either:

        val result = if ProgramMode.isInNormalMode && classTag[M] == classTag[HardDriveModel] then
          val ec = toHardDrive(model.asInstanceOf[HardDriveModel])
          repos.diskRepo.insertOrUpdate(ec)
        else if ProgramMode.isInHardDriveMode && classTag[M] == classTag[HardDriveModel] then
          val ec = toWiping(model.asInstanceOf[HardDriveModel])
          repos.wipingRepo.insertOrUpdate(ec)
        else
          val ec = toInfo(model.asInstanceOf[GeneralInfoModel])
          repos.infoRepo.insertOrUpdate(ec.ok())

      


  end insertOrUpdateModel

  override def insertModel[M <: HLModel : ClassTag](model: M)(using hardwareGrabberService: HardwareGrabberService): Either[HLDatabaseServiceException, Unit] =
    model match
      case info: GeneralInfoModel => addPCRecord(info)
      case _ => addWipingRecords(genID, model.asInstanceOf[HardDriveModel])

  override def addPCRecord(info: GeneralInfoModel)(using hardwareGrabberService: HardwareGrabberService): Either[HLDatabaseServiceException, Unit] =
    transact:
      ox.either:
        if doesPCRecordExist(info) then
          Right(HLDatabaseServiceException.duplicateEntry(NameOf.nameOfType[GeneralInfoModel], info.serial))
        else
          val ec = toInfo(info)
          repos.infoRepo.insertOrUpdate(ec.ok())


  def addWipingRecords(itsaID: String, drives: HardDriveModel*): Either[HLDatabaseServiceException, Unit] =
    val updatedDrives = drives.map: drive =>
      drive.copy(itsaID = drive.itsaID ?? itsaID)
    transact:
      ox.either:
        val records = updatedDrives.map(toWiping)
        records.map(repos.wipingRepo.insertOrUpdate).map: item =>
          val result = item.left.map(code => HLDatabaseServiceException.apply(code).fail())

          HLDatabaseServiceException.unknownError("Unknown error has occurred when inserting or updating record in DB.").fail()
  end addWipingRecords

  override def findDriveBySerialNumber(serial: String): Either[HLDatabaseServiceException, HardDriveModel] =
    transact:
      ox.either:
        repos.diskRepo.findDiskRecord(serial) match
          case None => HLDatabaseServiceException.noEntryFound("Disk Record", serial).fail()
          case Some(value) => value |> createHardDriveModelFromDisk

  override def findWipingRecordByID(itsaID: String): Either[HLDatabaseServiceException, HardDriveModel] =
    transact:
      ox.either:
        repos.wipingRepo.findWipingRecord(itsaID) match
          case None => HLDatabaseServiceException.noEntryFound("Wiping Record", itsaID).fail()
          case Some(value) => value |> createHardDriveModel

  def toWiping(model: HardDriveModel): WipingCreator =


    /// todo get if its an ssd
    //
    //    val lsblk = Lsblk()(using Givens.HLProcessConfig)
    //    lsblk()(model.)

    new WipingCreator(
      hddID = model.itsaID,
      serial = model.serial,
      model = model.model,
      insertionDate = OffsetDateTime.now(),
      capacity = model.size.dbString |> Option[String],
      `type` = model.`type`,
      description = model.description,
      health = model.health.toString.toByte,
      toUpdate = true, isSsd = true, formFactor = ""
    )

  end toWiping

  given [U]: Conversion[U, Option[U]] with
    override def apply(x: U): Option[U] = Some(x)


  override def markAllRowsWithIDInTableAsError[M <: HLModel : ClassTag](id: String): Either[HLDatabaseServiceException, Unit] = ???


  override final def replaceWithIDOrMarkAsErrorInDB(oldID: String, newID: String): Unit =
    () //TODO: See if this needs implementing.


  private def markAllRowsWithIDAsError[EC <: HLEntityCreatorWithSomeKindOfID, E <: HLEntityWithSomeKindOfID, R <: HLRepoWithID[EC, E]](id: String)(using repo: R, dbCodec: DbCodec[E], dbCon: DbCon, tableInfo: HLTableInfo[EC, E]): Either[HLDatabaseServiceException, Unit] =
    import org.itsadigitaltrust.common.Operators.or
    ox.either:
      logger.info(s"Marking rows with id: $id as error!")
      if id == null || id.isEmpty then
        Right[HLDatabaseServiceException, Unit](())
      else
        val nonErrorRows = repo.findAllByItsaID(id)
        val allRows = repo.findAllByIDStartingWith(id) or Vector.empty[Option[E]] |> Option.apply
        val numberOfErrorRows = Math.abs(allRows.size - nonErrorRows.size)
        if numberOfErrorRows > 0 then
          val errorIndices = Range(numberOfErrorRows, allRows.size + 1)
          val newIDs = errorIndices.map: index =>
            s"${if id.length >= 2 && id(id.length - 2) == '.' then id else s"$id.0"}-E$index"
          val oldToNew = nonErrorRows.flatten.map(_.map(_.id)).zip(newIDs)
          oldToNew.foreach: item =>
            val id = item._1 ?? HLDatabaseServiceException.unknownError("Unknown error has occurred when marking rows as error!").fail()
            repo.replaceIDByPrimaryKey(id, item._2)(using summon[DbCon])
        end if
      end if
  end markAllRowsWithIDAsError


  override final def markAllRowsWithIDInDBAsError(itsaID: String): HLDBTransaction[Unit] =
    transact:
      markAllRowsWithIDAsError(itsaID)(using repos.infoRepo, summon[magnum.DbCodec[Info]])

      markAllRowsWithIDAsError(itsaID)(using repos.memoryRepo, summon[magnum.DbCodec[Memory]])
      markAllRowsWithIDAsError(itsaID)(using repos.mediaRepo, summon[magnum.DbCodec[Media]])
      markAllRowsWithIDAsError(itsaID)(using repos.diskRepo, summon[magnum.DbCodec[Disk]])
      markAllRowsWithIDAsError(itsaID)(using repos.wipingRepo, summon[magnum.DbCodec[Wiping]])


  end markAllRowsWithIDInDBAsError


  private lazy val letters = 'A' to 'Z'
  private final val lettersInTheAlphabet = 26


  // Supports up to ZZ (702 letters) for the hddID prefix
  //    val newDisks: Seq[WipingCreator] =
  //      val foundDrives: Seq[WipingCreator] = findAllByIdStartingWith[WipingCreator](disks.head.hddID).map(_.map(WipingCreator.apply)) ?? Seq.empty[WipingCreator]
  //      val suffixStartIndex = if foundDrives.size > 1 then foundDrives.size else 0
  //      if foundDrives.size == 0 then
  //        // If no drives found, we can use the first drive as is
  //        disks.toSeq
  //      else if foundDrives.size > lettersInTheAlphabet then
  //        // If more than 26 drives found, we need to add a prefix
  //        val times = foundDrives.size / lettersInTheAlphabet
  //        val prefix = letters(times - 1) + letters(foundDrives.size % lettersInTheAlphabet)
  //        disks.map(_.copy(hddID = disks.head.hddID + prefix))
  //      else if foundDrives.size == 1 then
  //        // If only one drive found, we can use the first drive as is
  //        disks.map(_.copy(hddID = disks.head.hddID + letters(suffixStartIndex)))
  //      else
  //        // If multiple drives found, we need to add a prefix to each drive
  //        disks.indices.map: index =>
  //          disks(index).copy(hddID = disks.head.hddID + letters(suffixStartIndex + disks.indexOf(index)))
  //
  ////      boundary:
  //        val allDisks: Seq[WipingCreator] =
  //          if disks.size == 1 then
  //            val foundDisks: Seq[WipingCreator] = findAllByIdStartingWith[WipingCreator](disks.head.hddID) match
  //              case Some(value) => value.map(WipingCreator.apply)
  //              case None => Seq.empty[WipingCreator]
  //            if foundDisks.isEmpty then
  //              if foundDisks.size > lettersInTheAlphabet then
  //                val times = foundDisks.size / lettersInTheAlphabet
  //                val prefix = letters(times - 1) + letters(foundDisks.size % lettersInTheAlphabet)
  //                Seq(disks.head.copy(hddID = disks.head.hddID + prefix))
  //              else
  //                Seq(disks.head.copy(hddID = disks.head.hddID + letters(foundDisks.size)))
  //            else
  //              disks
  //          else
  //            disks
  //        end allDisks
  //        if allDisks != disks then
  //          boundary.break(allDisks)

  //        val noIDDrives = disks.groupBy(_.hddID) //filter(d => d.hddID.isEmpty || d.hddID == "NOT LOGGED")
  //        noIDDrives.values.flatMap: driveGroup =>
  //          if driveGroup.size >= 2 then
  //            val drives = driveGroup.drop(1)
  //            val first = drives.head
  //            val mappedDisks = (1 until drives.size).map: index =>
  //              val i = index - 1 // Start from 0
  //              val times = if i > 26 then i / lettersInTheAlphabet else 0
  //              val prefix = if times > 0 then letters(times - 1) + letters(i) else letters(i)
  //              val newDisk = drives(index).copy(hddID = drives(index).hddID + prefix)
  //              logger.info(s"New Disk: $newDisk")
  //              newDisk
  //            Seq(first) ++ mappedDisks
  //          else
  //            disks.toSeq
  //        .toSeq
  //      eitherTransact[Seq[WipingCreator], Wiping,WipingCreator](dataSource,HLDBErrorCode.UnknownError,wipingTable):
  //        markAllRowsMatchingRecordsWithIDAsError(newDisks*)
  //      .get


  def addWipingRecords(disks: WipingCreator*): HLDBTransaction[Unit] =
    // This supports up to 702 drives with the same ID prefix, e.g. "HDD1234A", "HDD1234B", ..., "HDD1234ZZ"
    def getSuffix(start: Int, current: Int): String =
      val index = start + current
      if index > lettersInTheAlphabet then
        val times = index / lettersInTheAlphabet
        s"${letters(times - 1)}${letters(index % lettersInTheAlphabet)}"

      else
        letters(index).toString
    end getSuffix

    given ct: ClassTag[WipingCreator] = classTag[WipingCreator]

    logger.info(s"Wiping disks: $disks")
    transact:
      ox.either:
        val result = findAllEntitiesStartingWithIDInTable[WipingCreator, Wiping](disks.head.hddID)
        val foundDrives: Vector[EntityFromEC[ECFromEntity[Wiping]]] = result ??
          HLDatabaseServiceException.noEntriesFound(disks.head.hddID).fail()

        val suffixStartIndex = if foundDrives.size > 1 then foundDrives.size else 0
        val newDisks: Seq[WipingCreator] =
          if suffixStartIndex > 0 then
            logger.info(s"Found ${foundDrives.size} drives with the same ID prefix: ${disks.head.hddID}")
            disks.indices.map: index =>
              val suffix = getSuffix(suffixStartIndex, index)
              disks(index).copy(hddID = disks.head.hddID + suffix)

          else
            logger.info(s"No drives found with the same ID prefix: ${disks.head.hddID}")
            disks.indices.map: index =>
              val suffix = if index == 0 then "" else getSuffix(0, index - 1)
              disks(index).copy(hddID = disks.head.hddID + suffix)
        end newDisks


        newDisks.map(_.getItsaID).map(_ => markAllRowsWithIDAsError[WipingCreator, Wiping, repos.WipingRepo](_))

  end addWipingRecords

  private def findAllEntitiesStartingWithIDInTable[EC <: HLEntityCreatorWithSomeKindOfID : ClassTag, E <: HLEntityWithSomeKindOfID](itsaID: String)(using repo: HLRepoWithID[EC, E], dbCodec: DbCodec[E], table: HLTableInfo[EC, E], dbTx: DbTx): Either[HLDatabaseServiceException, Vector[E]] =
    ox.either:
      val result =
        repo.findAllByIDStartingWith(itsaID).map: item =>
          item
        .filter(_.isDefined)
        .map(_.get)
      result ?? HLDatabaseServiceException.noEntriesFound(itsaID).fail()

  override def findByID[M <: HLModel : ClassTag](itsaID: String)(using hardwareGrabberService: HardwareGrabberService): Either[HLDatabaseServiceException, M] =
        def notFound: HLDatabaseServiceException =
          HLDatabaseServiceException.noEntryFound(classTag[M].runtimeClass.getSimpleName, itsaID)

        def dbError(code: HLDBErrorCode): HLDatabaseServiceException =
          HLDatabaseServiceException(code, classTag[M].runtimeClass.getSimpleName, itsaID)

        def asRequestedModel[A <: HLModel](result: Either[HLDBErrorCode, A]): Either[HLDatabaseServiceException, M] =
          result
            .left
            .map(dbError)
            .map(_.asInstanceOf[M])

        transact:
          if classTag[M] == classTag[HardDriveModel] then
            if ProgramMode.isInNormalMode then
              asRequestedModel:
                repos.diskRepo
                  .findFirstByItsaID(itsaID)
                  .map(createHardDriveModelFromDisk)
            else
              asRequestedModel:
                repos.wipingRepo
                  .findFirstByItsaID(itsaID)
                  .map(createHardDriveModel)

          else if classTag[M] == classTag[MemoryModel] then
            asRequestedModel:
              repos.memoryRepo
                .findFirstByItsaID(itsaID)
                .map(createMemoryModel)

          else if classTag[M] == classTag[MediaModel] then
            asRequestedModel:
              repos.mediaRepo
                .findFirstByItsaID(itsaID)
                .map(createMediaModel)

          else if classTag[M] == classTag[ProcessorModel] then
            asRequestedModel:
              repos.infoRepo
                .findFirstByItsaID(itsaID)
                .map: processor =>
                  ProcessorModel(
                    name = processor.genId,
                    frequency = Frequency(processor.cpuSpeed),
                    shortDescription = processor.cpuVendor ?? "",
                    longDescription = processor.cpuDescription,
                    serial = processor.cpuSerial ?? "",
                    width = processor.cpuWidth.map(_.toInt) ?? 64,
                    cores = processor.cpuCores.map(_.toInt) ?? 1,
                    threads = hardwareGrabberService.processors.map(_.headOption.map(_.threads) ?? 0) ?? 0
                  )

          else
            Left(notFound)
          end if
  end findByID



  override final def stop(): Unit = ()

  given Conversion[OffsetDateTime, java.sql.Timestamp] with
    override def apply(x: OffsetDateTime): Timestamp =
      Timestamp.from(x.toInstant)

  protected var processor: Option[ProcessorModel] = None

  private def createHardDriveModel(hardDrive: Wiping) =
    HardDriveModel(
      model = hardDrive.model,
      size = DataSize(0, DataSizeUnit.GB),
      serial = hardDrive.serial,
      description = hardDrive.description ?? "",
      health = Percentage(100),
      performance = Percentage(100),
      connectionType = HardDriveConnectionType.NVME
    )

  private inline def createHardDriveModel(disk: Disk): HardDriveModel =
    createHardDriveModelFromDisk(disk)

  private def createHardDriveModelFromDisk(hardDrive: Disk) =

    HardDriveModel(
      model = hardDrive.model,
      size = DataSize(0, DataSizeUnit.GB),
      serial = hardDrive.serial,
      description = hardDrive.description ?? "",
      health = Percentage(100),
      performance = Percentage(100),
      connectionType = HardDriveConnectionType.NVME
    )
  private def createInfoModel[M <: HLModel : ClassTag](info: Info) =
    GeneralInfoModel(
      computerID = info.genId,
      itsaID = info.itsaID,
      vendor = info.cpuVendor ?? "",
      serial = info.cpuSerial ?? "",
      os = info.os ?? "",
      model = info.cpuProduct ?? "",
      description = info.genDesc,
    )

  end createInfoModel


  protected def createMemoryModel(memory: Memory) =
    MemoryModel(
      size = DataSize.from(memory.size) ?? DataSize(0, DataSizeUnit.GB),
      description = memory.description ?? ""
    )
  protected def createMediaModel(media: Media) =
    MediaModel(
      description = media.descr ?? "",
      handle = media.handle ?? ""
    )
  protected def toMemory(memoryModel: MemoryModel): MemoryCreator =
    MemoryCreator(memoryModel.size.dbString, itsaID, memoryModel.description)

  protected def toHardDrive(hardDriveModel: HardDriveModel): DiskCreator =
    DiskCreator(itsaID, hardDriveModel.model, hardDriveModel.size.dbString, hardDriveModel.serial, hardDriveModel.connectionType.toString, "ATA Disk")


  protected def toInfo(infoModel: GeneralInfoModel)(using hardwareGrabberService: HardwareGrabberService): Either[Error, InfoCreator] =
    ox.either:
      val totalMemoryResult = hardwareGrabberService.memory.map: mem =>
        mem.map(m => m.size)
      ??
        HLDatabaseServiceException.unknownError("Could not get total memory").fail()
        
      val totalMemory: Seq[Double] = totalMemoryResult.map(_.map(_.toDouble)) ?? Seq.empty[Double]
      
      
      this.processor = hardwareGrabberService.processors.map(_.head) ?? HLDatabaseServiceException.unknownError("Could not get processor").fail()
      val processor = this.processor.get
  
      InfoCreator(cpuVendor = infoModel.vendor,
        genId = genID + s" ${BuildInfo.version}",
        itsaID = itsaID, cpuSerial = Some(processor.serial),
        insertionDate = OffsetDateTime.now(),
        lastUpdated = OffsetDateTime.now(),
        os = infoModel.os,
        cpuProduct = infoModel.model,
        cpuSpeed = processor.frequency.dbString,
        cpuCores = processor.cores.toString,
        cpuWidth = processor.width.toString,
        cpuDescription = processor.longDescription,
        genDesc = infoModel.description,
        genProduct = infoModel.computerID,
        genVendor = infoModel.vendor,
        genSerial = infoModel.serial,
        totalMemory = totalMemory.toString.toOption
      )

end MySQLHardwareLoggerDatabaseService

object  MySQLHardwareLoggerDatabaseService:
  //from
  // db.username=itsa
  //db.password=itsa123
  //db.name=hwlogger
  //db.port=3306
  //db.unit7.address=10.0.1.230
  //db.unit9.address=192.168.100.230
  //db.local.address=localhost
  //
  //#Time in seconds
  //db.timeout=1
  //
  //db.autoReconnect=true
  //db.maxReconnects=3
  //db.useLocalhost=true
  //db.localhost.username=root
  //db.localhost.password=root

end MySQLHardwareLoggerDatabaseService
