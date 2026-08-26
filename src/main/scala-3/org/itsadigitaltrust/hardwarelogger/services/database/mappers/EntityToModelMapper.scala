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
import org.itsadigitaltrust.hardwarelogger.backend.database.{EntityCreatorToEntityMapper, HLDatabase, RepoFromEntityCreator, UpdateOrInsert}
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
import scala.util.NotGiven

private[database] type ToEntityCreator[M <: HLModel, EC <: HLEntityCreator] = Conversion[M, Either[HLDatabaseServiceExceptionBase, EC]]
private[database] type FromEntity[E <: HLEntity, M <: HLModel] = Conversion[E, M]


  
  
  private[database] trait HLModelToBackendMapper[ModeType <: ProgramOperationMode, M <: HLModel](using backend: HLDatabase) extends GenericTypeMapper[M, HLEntityCreatorWithSomeKindOfID],
    HLDBOperationLifecycle[ModeType, M], HWLLoggable:
    type Out2 <: HLEntityWithSomeKindOfID


    private[database] given repo: RepoFromEntityCreator[Out, Out2] = deferred
  
    given ecMapper: (EntityCreatorToEntityMapper[Out] {type Out = Out2}) = deferred
  
    private[database] given toCreator: ToEntityCreator[M, Out] = deferred
  
    private[database] given toModel: FromEntity[Out2, M] = deferred

  
  end HLModelToBackendMapper
  
  private[database] object HLModelToBackendMappers:
  
    import EntityCreatorModelConversions.given
    private given requiresReadingItsaID(using backend: HLDatabase): RequiresReadingItsaID with
      override def itsaID: String = NotificationCentre.getFetchedData.flatMap(_.itsaID) ?? ""


    given generalInfoToInfoCreator(using backend: HLDatabase): HLModelToBackendMapper[NormalMode, GeneralInfoModel] with
      override type Out = InfoCreator
      override type Out2 = Info

      /**
       * Phase 3: Synchronous DB Mutation Target.
       * Executes explicit row additions or structural deletions.
       */
      override def executeSyncRules(models: GeneralInfoModel*)(using Ox): Result[Seq[GeneralInfoModel]] =
        ox.either:
          backend.insertAll(models.map(toCreator.convert).map(_.ok()))(using ecMapper).left.map(HLDatabaseServiceException()).ok()
          models


      override def provideValidationContext(models: GeneralInfoModel*)(using context: GlobalValidationContext[? <: HLModel])(using Ox): Unit =
        models.foreach: model =>

          model.itsaID.foreach: itsaID =>
            val result = backend.findFirstByItsaID(itsaID)
            if result.isRight then
              context += ValidationIssue(
                source = "General Info Model",
                message = s"Entity with '$itsaID' already exists"
              )

      private[database] def findAllSuffixedWithErrorNumber(itsaID: String): HLDatabaseOperations.Result[Seq[GeneralInfoModel]] =
        ox.either:
          val result = backend.findAllStartingWithItsaID(itsaID).left.map(HLDatabaseServiceException()).ok()
          result.map(toModel.convert)

    end generalInfoToInfoCreator




    given memoryToInfoCreator(using backend: HLDatabase): HLModelToBackendMapper[NormalMode, MemoryModel] with
      override type Out = MemoryCreator
      override type Out2 = Memory

      
      override def provideValidationContext(models: MemoryModel*)(using context: GlobalValidationContext[? <: HLModel])(using Ox): Unit =
        ()
      
      override def executeSyncRules(models: MemoryModel*)(using Ox): Result[Seq[MemoryModel]] =
        ox.either:
          backend.insertAll(models.map(toCreator.convert).map(_.ok()))(using ecMapper).left.map(HLDatabaseServiceException()).ok()
          models

    end memoryToInfoCreator
  
    given processorToInfoCreator(using backend: HLDatabase): HLModelToBackendMapper[NormalMode, ProcessorModel] with
      override type Out = InfoCreator
      override type Out2 = Info

      override def provideValidationContext(models: ProcessorModel*)(using context: GlobalValidationContext[? <: HLModel])(using Ox): Unit =
        ()

      override def executeSyncRules(models: ProcessorModel*)(using Ox): Result[Seq[ProcessorModel]] =
        ox.either:
          backend.insertAll(models.map(toCreator.convert).map(_.ok()))(using ecMapper).left.map(HLDatabaseServiceException()).ok()
          models

    end processorToInfoCreator


    given mediaToMediaCreator(using backend: HLDatabase): HLModelToBackendMapper[NormalMode, MediaModel] with
      override type Out = MediaCreator
      override type Out2 = Media


      override def provideValidationContext(models: MediaModel*)(using context: GlobalValidationContext[_ <: HLModel])(using Ox): Unit =
        ()
      override def executeSyncRules(models: MediaModel*)(using Ox): Result[Seq[MediaModel]] =
        ox.either:
          backend.insertAll(models.map(toCreator.convert).map(_.ok()))(using ecMapper).left.map(HLDatabaseServiceException()).ok()
          models
    end mediaToMediaCreator


    given hardDriveToDiskCreator(using backend: HLDatabase): HLModelToBackendMapper[NormalMode, HardDriveModel] with
      override type Out = DiskCreator
      override type Out2 = Disk


      override def provideValidationContext(models: HardDriveModel*)(using context: GlobalValidationContext[? <: HLModel])(using Ox): Unit =
        models.foreach: model =>
          val hasWipingLog = backend.findBySerialNumber(model.serial).isRight

          if hasWipingLog then
            context += ValidationIssue(
              source = "Wiping Table Check",
              message = s"Hard drive with serial number ${model.serial} does not exist in the wiping table.",
              isSoftWarning = true
            )
            logger.warn(s"Hard drive with serial number ${model.serial} does not exist in the wiping table.")

      end provideValidationContext


      override def executeSyncRules(models: HardDriveModel*)(using Ox): Result[Seq[HardDriveModel]] =
        ox.either:
          val itsaID = requiresReadingItsaID.itsaID
          val drivesInComputer = backend.findAllByItsaID(itsaID).toOption ?? Vector()
          val notInComputer = models.filterNot(model => drivesInComputer.exists(_.serial == model.serial))
           backend.deleteByAllSerials(notInComputer.map(_.serial)*).left.map(HLDatabaseServiceException()).ok()
          models.map: model =>
            backend.insert((model.copy(itsaID = itsaID) |> toCreator.convert).ok())
            model
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
        models.foreach: model =>
          if backend.findBySerialNumber(model.serial).isLeft then
            context += ValidationIssue(
              source = "Wiping Table Check",
              message = s"Hard drive with serial number ${model.serial} already exists in the wiping table.",
              isSoftWarning = true
            )
            logger.warn(s"Hard drive with serial number ${model.serial} already exists in the wiping table.")

          backend.findAllByItsaID(model.itsaID).map:
            _.foreach: disk =>
              context += ValidationIssue(
                source = "Wiping Table Check",
                message = s"'${model.serial} & '${disk.serial}' both have the ID of '${model.itsaID}'.",
                isSoftWarning = true
              )


      end provideValidationContext

      override def executeSyncRules(models: HardDriveModel*)(using Ox): Result[Seq[HardDriveModel]] =
        val itsaID = requiresReadingItsaID.itsaID
        ox.either:
          models.foreach: model =>
            backend.findAllByItsaID(model.itsaID).map:
              _.foreach: disk =>
                toCreator.convert(model).map: ec =>
                  val oldItsaID = backend.findItsaIDBySerialNumber(model.serial)
                  oldItsaID.map: old =>
                    backend.replaceItsaID(old, itsaID)
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


