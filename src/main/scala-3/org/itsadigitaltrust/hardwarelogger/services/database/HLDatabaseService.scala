package org.itsadigitaltrust.hardwarelogger.services.database

import com.augustnagro.magnum
import com.augustnagro.magnum.{DbCodec, DbCon, DbTx}
import com.mysql.cj.jdbc.MysqlDataSource
import com.mysql.cj.jdbc.exceptions.{MySQLQueryInterruptedException, MySQLTimeoutException}
import com.somainer.nameof.NameOf
import org.itsadigitaltrust.common.Operators.{?, ??, toOption, |>, ||>}
import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.common.{optional, toVectorOfOptions}
import org.itsadigitaltrust.common.optional.!
import org.itsadigitaltrust.common.types.DataSizeType.numericDataSize.mkNumericOps
import org.itsadigitaltrust.common.types.DataSizeType.{DataSize, DataSizeUnit}
import org.itsadigitaltrust.common.types.FrequencyType.Frequency
import org.itsadigitaltrust.common.types.PercentageType.Percentage
import org.itsadigitaltrust.hardwarelogger.backend.{ConnectionErrorType, DataSourceLoaderService, HLDBErrorCode, HLSqlDataSource, HLTableInfo, repos}
import org.itsadigitaltrust.hardwarelogger.backend.entities.*
import org.itsadigitaltrust.hardwarelogger.backend.repos.{DiskRepo, HLRepoWithID, InfoRepo, MediaRepo, MemoryRepo, WipingRepo}
import org.itsadigitaltrust.hardwarelogger.backend.utils.IPAddressFinder
import org.itsadigitaltrust.hardwarelogger.core.BuildInfo
import org.itsadigitaltrust.hardwarelogger.delegates.{HardDriveMode, NormalMode, ProgramMode, ProgramOperationMode}
import org.itsadigitaltrust.hardwarelogger.models.*
import org.itsadigitaltrust.hardwarelogger.services.{HLFrontendException, HardwareGrabberService}
import org.itsadigitaltrust.hardwarelogger.services.OshiHardwareGrabberApplicationService.HardwareGrabberException
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{NotificationCentre, RequiresItsaID}
import org.itsadigitaltrust.hardwarelogger.backend.database.{EntityCreatorToEntityMapper, HLDatabase, RepoFromEntityCreator}
import org.itsadigitaltrust.hardwarelogger.services.database.mappers.{HLDatabaseMappersAndGivens, HLModelToBackendMapper, HLModelToBackendMappers, OptionalHLModelToEntityMapper}
import org.itsadigitaltrust.hardwarelogger.services.validation.GlobalValidationContext
import org.itsadigitaltrust.hardwarelogger.shared.HardwareLoggerSharedData
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.MemoryTableRowViewModel
import ox.Ox
import ox.either.{fail, ok}

import java.time.OffsetDateTime
import scala.Conversion.into
import scala.collection.mutable
import scala.compiletime.{deferred, summonInline}
import scala.reflect.{ClassTag, classTag}
import scala.util.boundary.Label
import scala.util.NotGiven

object HLDatabaseService:
  import HLDatabaseServiceCoordinator.given

trait HLDatabaseService(using localhost: Boolean,  val backend: HLDatabase, dataSourceLoaderService: DataSourceLoaderService[HLSqlDataSource])
  extends RequiresItsaID, HLDatabaseMappersAndGivens:
  import HLDatabaseService.*

  type HLDBTransaction[T] = Either[HLDatabaseServiceExceptionBase, T]
//
//  def testConnection(): Boolean = false
//
  def connect()(using ox.Ox): HLDBTransaction[Unit]
//
//


  def runValidation[M <: HLModel : ClassTag](models: M*)(using context: HLDatabaseServiceCoordinatorOperationContext = HLDatabaseServiceCoordinator.programModeBasedContext)(using globalValidationContext: GlobalValidationContext[? <: HLModel])(using ox.Ox): HLDBTransaction[Unit]
  def executeFinalSave[M <: HLModel : ClassTag](models: M*)(using context: HLDatabaseServiceCoordinatorOperationContext = HLDatabaseServiceCoordinator.programModeBasedContext)(using ox.Ox): HLDBTransaction[Unit]





  def replaceWithIDOrMarkAsErrorInDB(oldID: String, newID: String): Unit

  def markAllRowsWithIDInTableAsError[M <: HLModel : ClassTag](id: String)(using context: HLDatabaseServiceCoordinatorOperationContext = HLDatabaseServiceCoordinator.programModeBasedContext)(using ox.Ox): HLDBTransaction[Unit]

  def doesPCRecordExist(info: GeneralInfoModel)(using hardwareGrabberService: HardwareGrabberService)(using context: HLDatabaseServiceCoordinatorOperationContext = HLDatabaseServiceCoordinator.programModeBasedContext): Boolean


  /**
   * This does not include the wiping table.
   */
  def markAllRowsWithIDInDBAsError(itsaID: String)(using context: HLDatabaseServiceCoordinatorOperationContext = HLDatabaseServiceCoordinator.programModeBasedContext)(using ox.Ox): HLDBTransaction[Unit]

  def findByID[M <: HLModel : ClassTag](itsaID: String)(using hardwareGrabberService: HardwareGrabberService)(using context: HLDatabaseServiceCoordinatorOperationContext = HLDatabaseServiceCoordinator.programModeBasedContext)(using ox.Ox): HLDBTransaction[M]
  
  def findBySerialNumber[M <: HLModel : ClassTag](serial: String)(using HardwareGrabberService)(using context: HLDatabaseServiceCoordinatorOperationContext = HLDatabaseServiceCoordinator.programModeBasedContext)(using ox.Ox): HLDBTransaction[M]

  def findAllInfoModelsWithErrorNumber(withID: String)(using HardwareGrabberService)(using context: HLDatabaseServiceCoordinatorOperationContext = HLDatabaseServiceCoordinator.programModeBasedContext)(using ox.Ox): HLDBTransaction[Vector[GeneralInfoModel]]

  //
//  def insert[M <: HLModel : ClassTag](model: M)(using hardwareGrabberService: HardwareGrabberService)(using context: HLDatabaseServiceCoordinatorOperationContext = HLDatabaseServiceCoordinator.programModeBasedContext)(using ox.Ox): HLDBTransaction[Unit]
//
//
//  def insertAll[M <: HLModel](models: M*)(using hardwareGrabberService: HardwareGrabberService)(using context: HLDatabaseServiceCoordinatorOperationContext = HLDatabaseServiceCoordinator.programModeBasedContext)(using ox.Ox): HLDBTransaction[Unit]

  //

  def updateAndInsertAll[M <: HLModel : ClassTag](models: M*)(using hardwareGrabberService: HardwareGrabberService)(using context: HLDatabaseServiceCoordinatorOperationContext = HLDatabaseServiceCoordinator.programModeBasedContext)(using ox.Ox): HLDBTransaction[Unit]


  def stop(): Unit

end HLDatabaseService


class MySQLHardwareLoggerDatabaseService(using localhost: Boolean = false, override val backend: HLDatabase, dataSourceLoaderService: DataSourceLoaderService[HLSqlDataSource])
  extends HLDatabaseService with HWLLoggable with HLDatabaseMappersAndGivens:

  import HLDatabase.given
  import HLModelToBackendMappers.*

  override type Error = HLDatabaseServiceExceptionBase
  type HLDBTransaction[T] = Either[Error, T]
  /**
   * Combined transaction body function that has access to both DbTx (magnum context)
   * and the ox.either error handling context via Label.
   */
  type CombinedTransactionBody[T] = DbTx ?=> Label[HLDBTransaction[T]] ?=> HLDBTransaction[T]
  //  given infoFinder: FindByID[GeneralInfoModel] with
  //    def find(itsaID: String)(using dbTx: DbTx, hardwareGrabberService: HardwareGrabberService): Either[HLDatabaseServiceExceptionBase, GeneralInfoModel] =
  //      mapDbError(itsaID):
  //        repos.infoRepo.findByItsaID(itsaID)
  //          .map(createInfoModel)
  private final val lettersInTheAlphabet = 26

  import MySQLHardwareLoggerDatabaseService.given
  import backend.given




  inline given getMapper[Mode <: ProgramOperationMode, M <: HLModel : ClassTag](using context: HLDatabaseServiceCoordinatorOperationContext = HLDatabaseServiceCoordinator.programModeBasedContext): HLModelToBackendMapper[Mode, M] =
    if classTag[M].runtimeClass == classOf[GeneralInfoModel] then
      generalInfoToInfoCreator.asInstanceOf[HLModelToBackendMapper[Mode, M]]
    else if classTag[M].runtimeClass == classOf[MemoryModel] then
      memoryToInfoCreator.asInstanceOf[HLModelToBackendMapper[Mode, M]]
    else if classTag[M].runtimeClass == classOf[ProcessorModel] then
      processorToInfoCreator.asInstanceOf[HLModelToBackendMapper[Mode, M]]
    else if classTag[M].runtimeClass == classOf[MediaModel] then
      mediaToMediaCreator.asInstanceOf[HLModelToBackendMapper[Mode, M]]
    else if classTag[M].runtimeClass == classOf[HardDriveModel] && context.mode == "Normal" then
      hardDriveToDiskCreator.asInstanceOf[HLModelToBackendMapper[Mode, M]]
    else if classTag[M].runtimeClass == classOf[HardDriveModel] && context.mode == "HardDrive" then
      wipingToWipingCreator.asInstanceOf[HLModelToBackendMapper[Mode, M]]
    else throw HLFrontendException(s"No backend mapper found for model type ${classTag[M].runtimeClass.getName} in mode ${context.mode}")


  end getMapper


  private inline def getMapperMatchingMode[M <: HLModel: ClassTag](using inline context: HLDatabaseServiceCoordinatorOperationContext = HLDatabaseServiceCoordinator.programModeBasedContext) =
    inline context.mode match
      case "Normal" => getMapper[NormalMode, M]
      case "HardDrive" => getMapper[HardDriveMode, M]
      case _ => getMapper[NormalMode, M]
      

  private lazy val letters = 'A' to 'Z'
  val dataSource: Either[HLDatabaseServiceException, HLSqlDataSource] = dataSourceLoaderService.dataSource.left.map(HLDatabaseServiceException(_))

  end dataSource
  var transactionErrorHandler: Throwable => Unit = _ => ()
  private var noIDIndex: Option[Long] = None

  override final def replaceWithIDOrMarkAsErrorInDB(oldID: String, newID: String): Unit =
    () //TODO: See if this needs implementing.

  override final def markAllRowsWithIDInDBAsError(itsaID: String)(using context: HLDatabaseServiceCoordinatorOperationContext = HLDatabaseServiceCoordinator.programModeBasedContext)(using ox.Ox): HLDBTransaction[Unit] =
    Right(())

  def runValidation[M <: HLModel : ClassTag](models: M*)(using context: HLDatabaseServiceCoordinatorOperationContext = HLDatabaseServiceCoordinator.programModeBasedContext)(using globalValidationContext: GlobalValidationContext[? <: HLModel])(using ox.Ox): HLDBTransaction[Unit] =
    getMapperMatchingMode.provideValidationContext(models*)
    Right(())


  override def executeFinalSave[M <: HLModel : ClassTag](models: M*)(using context: HLDatabaseServiceCoordinatorOperationContext = HLDatabaseServiceCoordinator.programModeBasedContext)(using ox.Ox): HLDBTransaction[Unit] =
    ox.either:
      getMapper.executeSave(models*)
      ()


  override final def stop(): Unit = ()

  override def connect()(using ox.Ox): Either[HLDatabaseServiceException, Unit] =
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


  override def markAllRowsWithIDInTableAsError[M <: HLModel : ClassTag](id: String)(using context: HLDatabaseServiceCoordinatorOperationContext)(using Ox): HLDBTransaction[Unit] =
    Right(())
    
  override def findAllInfoModelsWithErrorNumber(withID: String)(using HardwareGrabberService)(using context: HLDatabaseServiceCoordinatorOperationContext = HLDatabaseServiceCoordinator.programModeBasedContext)(using ox.Ox): HLDBTransaction[Vector[GeneralInfoModel]] =
    ox.either:
      HLModelToBackendMappers.generalInfoToInfoCreator.findAllSuffixedWithErrorNumber(withID).ok().toVector

  override def doesPCRecordExist(info: GeneralInfoModel)(using hardwareGrabberService: HardwareGrabberService)(using context: HLDatabaseServiceCoordinatorOperationContext = HLDatabaseServiceCoordinator.programModeBasedContext): Boolean =
    info.itsaID.map: itsaID =>
      getMapperMatchingMode[GeneralInfoModel].findByItsaID(itsaID).isRight
    ?? false


  override def updateAndInsertAll[M <: HLModel : ClassTag](models: M*)(using hardwareGrabberService: HardwareGrabberService)(using context: HLDatabaseServiceCoordinatorOperationContext = HLDatabaseServiceCoordinator.programModeBasedContext)(using ox.Ox): HLDBTransaction[Unit] =
    ox.either:
      getMapper.updateAndInsertAll(models*)
      ()


  
  

  override def findByID[M <: HLModel: ClassTag](itsaID: String)(using hardwareGrabberService: HardwareGrabberService)(using context: HLDatabaseServiceCoordinatorOperationContext = HLDatabaseServiceCoordinator.programModeBasedContext)(using Ox): HLDBTransaction[M] =
    getMapperMatchingMode[M].findByItsaID(itsaID)

  end findByID


  def addWipingRecords(drives: Seq[HardDriveModel])(using context: HLDatabaseServiceCoordinatorOperationContext = HLDatabaseServiceCoordinator.programModeBasedContext): HLDBTransaction[Unit] =
//



//      NotificationCentre.sendUpdateSharedDataNotification(HardwareLoggerSharedData(drives = newDisks.?))

    ???

  end addWipingRecords

  override def findBySerialNumber[M <: HLModel : ClassTag](serial: String)(using HardwareGrabberService)(using context: HLDatabaseServiceCoordinatorOperationContext = HLDatabaseServiceCoordinator.programModeBasedContext)(using Ox): HLDBTransaction[M] =
    getMapper.findBySerial(serial)


  end findBySerialNumber
  


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



object MySQLHardwareLoggerDatabaseService:

  import HLDatabase.given


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



  export HLModelToBackendMappers.given


end MySQLHardwareLoggerDatabaseService
