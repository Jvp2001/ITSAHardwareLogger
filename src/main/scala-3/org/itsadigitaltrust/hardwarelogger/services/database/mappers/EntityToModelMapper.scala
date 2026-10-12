package org.itsadigitaltrust.hardwarelogger.services.database.mappers

import com.augustnagro.magnum.{DbCon, DbTx}
import org.itsadigitaltrust.common.GenericTypeMapper
import org.itsadigitaltrust.common.Operators.{?, ??, |>}
import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.common.types.DataSizeType.numericDataSize.mkNumericOps
import org.itsadigitaltrust.common.types.DataSizeType.{DataSize, DataSizeUnit}
import org.itsadigitaltrust.common.types.FrequencyType.Frequency
import org.itsadigitaltrust.common.types.PercentageType.Percentage
import org.itsadigitaltrust.hardwarelogger.backend.database.HLDatabase.given
import org.itsadigitaltrust.hardwarelogger.backend.database.{EntityCreatorToEntityMapper, HLDatabase, RepoFromEntityCreator, UpdateAndInsert}
import org.itsadigitaltrust.hardwarelogger.backend.HLDBErrorCode
import org.itsadigitaltrust.hardwarelogger.backend.entities.*
import org.itsadigitaltrust.hardwarelogger.backend.repos
import org.itsadigitaltrust.hardwarelogger.backend.repos.*
import org.itsadigitaltrust.hardwarelogger.core.BuildInfo
import org.itsadigitaltrust.hardwarelogger.delegates.{HardDriveMode, NormalMode, ProgramMode, ProgramOperationMode}
import org.itsadigitaltrust.hardwarelogger.models.HLModel
import org.itsadigitaltrust.hardwarelogger.models.HLModel.{GeneralInfoModel, HardDriveConnectionType, HardDriveModel, MediaModel, MemoryModel, ProcessorModel}
import org.itsadigitaltrust.hardwarelogger.services.HardwareGrabberService
import org.itsadigitaltrust.hardwarelogger.services.OshiHardwareGrabberApplicationService.HardwareGrabberException
import org.itsadigitaltrust.hardwarelogger.services.database.HLDatabaseOperations.Result
import org.itsadigitaltrust.hardwarelogger.services.database.{HLDBOperationLifecycle, HLDatabaseOperations, HLDatabaseService, HLDatabaseServiceException, HLDatabaseServiceExceptionBase}
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{NotificationCentre, RequiresItsaID, RequiresReadingItsaID}
import org.itsadigitaltrust.hardwarelogger.services.validation.{GlobalValidationContext, ValidationIssue}
import org.itsadigitaltrust.hardwarelogger.shared.HardwareLoggerSharedData
import ox.Ox
import ox.either.{fail, ok}

import java.sql.Timestamp
import java.time.OffsetDateTime
import scala.compiletime.deferred
import scala.util.{NotGiven, boundary}

private[database] type ToEntityCreator[M <: HLModel, EC <: HLEntityCreator] = Conversion[M, Either[HLDatabaseServiceExceptionBase, EC]]
private[database] type FromEntity[E <: HLEntity, M <: HLModel] = Conversion[E, M]


trait HLModelToBackendMapper[ModeType <: ProgramOperationMode, M <: HLModel](using backend: HLDatabase) extends GenericTypeMapper[M, HLEntityCreatorWithSomeKindOfID],
  HLDBOperationLifecycle[ModeType, M], HWLLoggable:
  type Out2 <: HLEntityWithSomeKindOfID

  given ecMapper: (EntityCreatorToEntityMapper[Out] {type Out = Out2}) = deferred

  private[database] given repo: RepoFromEntityCreator[Out, Out2] = deferred

  private[database] given toCreator: ToEntityCreator[M, Out] = deferred

  private[database] given toModel: FromEntity[Out2, M] = deferred

  private[database] given updateAndInsert[Out, Out2] = deferred


  override def provideValidationContext(models: M*)(using context: GlobalValidationContext[? <: HLModel])(using Ox): Unit =
    ()
  
  override def executeSyncRules(models: M*)(using Ox): Result[Seq[M]] =
    updateAndInsertAll(models*)

  private[database] def updateAndInsertAll(models: M*)(using Ox): Result[Seq[M]] =
    ox.either:
      if models.nonEmpty then
        given oldItsaID: Option[String] = NotificationCentre.getFetchedData.flatMap(_.oldItsaID)
        backend.updateAndInsertAll(models.map(toCreator.convert).map(_.ok())*).left.map(HLDatabaseServiceException()).ok()
      models

end HLModelToBackendMapper

object HLModelToBackendMappers:

  import EntityCreatorModelConversions.given
  import org.itsadigitaltrust.hardwarelogger.backend.database.RepoToEntityCreatorGivens.given

  private def nextErrorID[EC <: HLEntityCreatorWithSomeKindOfID](itsaID: String)(using backend: HLDatabase, mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out], ox: Ox): Either[HLDatabaseServiceExceptionBase, String] =
    val matchingIDs = backend.findAllStartingWithItsaID[EC](itsaID) match
      case Right(records) => records.map(_.getItsaID)
      case Left(HLDBErrorCode.NoEntriesFound) => Vector.empty
      case Left(error) => return Left(HLDatabaseServiceException(error))
    val nextNumber = matchingIDs.iterator
      .filter(_.startsWith(s"$itsaID-E"))
      .flatMap(_.drop(itsaID.length + 2).toIntOption)
      .maxOption
      .getOrElse(0) + 1
    Right(s"$itsaID-E$nextNumber")

  given requiresReadingItsaID(using backend: HLDatabase): RequiresReadingItsaID with
    override def itsaID: String = NotificationCentre.getFetchedData.flatMap(_.itsaID) ?? ""


  given generalInfoToInfoCreator(using backend: HLDatabase): HLModelToBackendMapper[NormalMode, GeneralInfoModel] with
    override type Out = InfoCreator
    override type Out2 = Info


    override def provideValidationContext(models: GeneralInfoModel*)(using context: GlobalValidationContext[? <: HLModel])(using Ox): Unit =
      val itsaID = requiresReadingItsaID.itsaID
      models.foreach: model =>
        val recordUsingID =
          if itsaID.nonEmpty then backend.findFirstByItsaID[InfoCreator](itsaID)
          else Left(HLDBErrorCode.NoEntriesFound)
        val recordForSerial = backend.findBySerialNumber[InfoCreator](model.serial)
        recordUsingID match
          case Right(existing) if existing.genSerial.getOrElse("") != model.serial =>
            context += ValidationIssue(
              source = "General Info Model",
              message = s"The ITSA ID '$itsaID' is already in use for a different computer. If you are sure it is correct, press Save; otherwise press Cancel.",
              isSoftWarning = true
            )
          case Left(error) if error != HLDBErrorCode.NoEntriesFound =>
            throw HLDatabaseServiceException(error)
          case _ => ()
        recordForSerial match
          case Right(existing) if existing.itsaID != itsaID =>
            context += ValidationIssue(
              source = "General Info Model",
              message = s"This computer is already recorded as '${existing.itsaID}'. Save to change its ITSA ID to '$itsaID'.",
              isSoftWarning = true
            )
          case Left(error) if error != HLDBErrorCode.NoEntriesFound =>
            throw HLDatabaseServiceException(error)
          case _ => ()

    override def executeSyncRules(models: GeneralInfoModel*)(using Ox): Result[Seq[GeneralInfoModel]] =
      ox.either:
        val itsaID = requiresReadingItsaID.itsaID
        models.foreach: model =>
          val owner = backend.findFirstByItsaID[InfoCreator](itsaID) match
            case Right(existing) if existing.genSerial.getOrElse("") != model.serial => Some(existing)
            case Right(_) | Left(HLDBErrorCode.NoEntriesFound) => None
            case Left(error) => HLDatabaseServiceException(error).fail()
          owner.foreach: existing =>
            val replacementID = nextErrorID[InfoCreator](itsaID).ok()
            backend.replaceItsaID[InfoCreator](itsaID, replacementID).left.map(HLDatabaseServiceException(_)).ok()

          backend.findBySerialNumber[InfoCreator](model.serial) match
            case Right(_) =>
              backend.updateItsaIDBySerial[InfoCreator](model.serial, itsaID).left.map(HLDatabaseServiceException(_)).ok()
            case Left(HLDBErrorCode.NoEntriesFound) =>
              val creator = toCreator.convert(model).left.map(identity).ok()
              backend.insert(creator).left.map(HLDatabaseServiceException(_)).ok()
            case Left(error) => HLDatabaseServiceException(error).fail()
        models


    private[database] def findAllSuffixedWithErrorNumber(itsaID: String): HLDatabaseOperations.Result[Seq[GeneralInfoModel]] =
      ox.either:
        val result = backend.findAllStartingWithItsaID(itsaID).left.map(HLDatabaseServiceException()).ok()
        result.map(toModel.convert)

  end generalInfoToInfoCreator


  given memoryToInfoCreator(using backend: HLDatabase): HLModelToBackendMapper[NormalMode, MemoryModel] with
    override type Out = MemoryCreator
    override type Out2 = Memory

  end memoryToInfoCreator

  given processorToInfoCreator(using backend: HLDatabase): HLModelToBackendMapper[NormalMode, ProcessorModel] with
    override type Out = InfoCreator
    override type Out2 = Info

    override def executeSyncRules(models: ProcessorModel*)(using Ox): Result[Seq[ProcessorModel]] =
      Right(models)

  end processorToInfoCreator


  given mediaToMediaCreator(using backend: HLDatabase): HLModelToBackendMapper[NormalMode, MediaModel] with
    override type Out = MediaCreator
    override type Out2 = Media


  end mediaToMediaCreator


  given hardDriveToDiskCreator(using backend: HLDatabase): HLModelToBackendMapper[NormalMode, HardDriveModel] with
    override type Out = DiskCreator
    override type Out2 = Disk


    override def provideValidationContext(models: HardDriveModel*)(using context: GlobalValidationContext[? <: HLModel])(using Ox): Unit =
      models.foreach: model =>
        backend.findBySerialNumber[WipingCreator](model.serial) match
          case Left(HLDBErrorCode.NoEntriesFound) =>
            context += ValidationIssue(
              source = "Wiping Table Check",
              message = s"Hard drive with serial number ${model.serial} does not exist in the wiping table.",
              isSoftWarning = true
            )
            logger.warn(s"Hard drive with serial number ${model.serial} does not exist in the wiping table.")
          case Left(error) => throw HLDatabaseServiceException(error)
          case Right(_) => ()

    end provideValidationContext


    override def executeSyncRules(models: HardDriveModel*)(using Ox): Result[Seq[HardDriveModel]] =
      ox.either:
        val itsaID = requiresReadingItsaID.itsaID
        backend.deleteByItsaID[DiskCreator](itsaID).left.map(HLDatabaseServiceException(_)).ok()
        backend.deleteByAllSerials[DiskCreator](models.map(_.serial)*).left.map(HLDatabaseServiceException(_)).ok()
        val diskRecords = models.map(model => toCreator.convert(model.copy(itsaID = itsaID)).left.map(identity).ok())
        backend.insertAll[DiskCreator](diskRecords).left.map(HLDatabaseServiceException(_)).ok()
        models

    end executeSyncRules

    /**
     * Phase 2: Core Table State Diagnostic Check.
     * Invoked immediately before and after writing mutations to log row metrics.
     */
    override def queryTableState(models: Seq[HardDriveModel], phase: hardDriveToDiskCreator.this.Phase)(using Ox): Unit =
      super.queryTableState(models, phase)


  end hardDriveToDiskCreator


  given wipingToWipingCreator(using backend: HLDatabase): HLModelToBackendMapper[HardDriveMode, HardDriveModel] with
    override type Out = WipingCreator
    override type Out2 = Wiping


    /**
     * Phase 1: Pre-Transaction Validation Analysis.
     * Runs under an active Ox context, enabling parallel database checks if needed.
     */
    override def provideValidationContext(models: HardDriveModel*)(using context: GlobalValidationContext[? <: HLModel])(using Ox): Unit =
      models.groupBy(_.itsaID).foreach:
        case (itsaID, drives) if itsaID.nonEmpty && drives.map(_.serial).distinct.size > 1 =>
          context += ValidationIssue(
            source = "Wiping Table Check",
            message = s"The HDD ID '$itsaID' is assigned to more than one hard drive in this save. Confirm the ID is correct before saving.",
            isSoftWarning = true
          )
        case _ => ()
      models.foreach: model =>
        val recordForSerial = backend.findBySerialNumber[WipingCreator](model.serial)
        val recordUsingID = backend.findFirstByItsaID[WipingCreator](model.itsaID)
        recordForSerial match
          case Right(existing) if existing.hddID != model.itsaID =>
            context += ValidationIssue(
              source = "Wiping Table Check",
              message = s"This hard drive is already recorded as '${existing.hddID}'. Save to change its HDD ID to '${model.itsaID}'.",
              isSoftWarning = true
            )
          case Left(error) if error != HLDBErrorCode.NoEntriesFound =>
            throw HLDatabaseServiceException(error)
          case _ => ()
        recordUsingID match
          case Right(existing) if existing.serial != model.serial =>
            context += ValidationIssue(
              source = "Wiping Table Check",
              message = s"The HDD ID '${model.itsaID}' is already in use for a different hard drive. Please check that the HDD ID is correct. If you are sure it is correct, press Save; otherwise press Cancel.",
              isSoftWarning = true
            )
          case Left(error) if error != HLDBErrorCode.NoEntriesFound =>
            throw HLDatabaseServiceException(error)
          case _ => ()

    override def executeSyncRules(models: HardDriveModel*)(using Ox): Result[Seq[HardDriveModel]] =
      ox.either:
        models.foreach: model =>
          val owner = backend.findFirstByItsaID[WipingCreator](model.itsaID) match
            case Right(existing) if existing.serial != model.serial => Some(existing)
            case Right(_) | Left(HLDBErrorCode.NoEntriesFound) => None
            case Left(error) => HLDatabaseServiceException(error).fail()
          owner.foreach: _ =>
            val replacementID = nextErrorID[WipingCreator](model.itsaID).ok()
            backend.replaceItsaID[WipingCreator](model.itsaID, replacementID).left.map(HLDatabaseServiceException(_)).ok()

          backend.findBySerialNumber[WipingCreator](model.serial) match
            case Right(_) =>
              backend.updateItsaIDBySerial[WipingCreator](model.serial, model.itsaID).left.map(HLDatabaseServiceException(_)).ok()
            case Left(HLDBErrorCode.NoEntriesFound) =>
              val creator = toCreator.convert(model).left.map(identity).ok()
              backend.insert(creator).left.map(HLDatabaseServiceException(_)).ok()
            case Left(error) => HLDatabaseServiceException(error).fail()
        models

  end wipingToWipingCreator

end HLModelToBackendMappers


private[database] trait HLDatabaseMappersAndGivens extends RequiresItsaID:
  self: org.itsadigitaltrust.hardwarelogger.services.database.HLDatabaseService =>

  import HLModelToBackendMappers.given


end HLDatabaseMappersAndGivens


trait OptionalHLModelToEntityMapper[Mode <: ProgramOperationMode, M <: HLModel]:
  def value: Option[HLModelToBackendMapper[Mode, M]]

object OptionalHLModelToEntityMapper:
  given found[Mode <: ProgramOperationMode, M <: HLModel](using mapper: HLModelToBackendMapper[Mode, M]): OptionalHLModelToEntityMapper[Mode, M] with
    def value: Option[HLModelToBackendMapper[Mode, M]] =
      Some(mapper)

  given missing[Mode <: ProgramOperationMode, M <: HLModel](using NotGiven[HLModelToBackendMapper[Mode, M]]): OptionalHLModelToEntityMapper[Mode, M] with
    def value: Option[HLModelToBackendMapper[Mode, M]] =
      None
