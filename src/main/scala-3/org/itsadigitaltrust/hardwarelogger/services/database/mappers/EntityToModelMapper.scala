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

  given requiresReadingItsaID(using backend: HLDatabase): RequiresReadingItsaID with
    override def itsaID: String = NotificationCentre.getFetchedData.flatMap(_.itsaID) ?? ""


  given generalInfoToInfoCreator(using backend: HLDatabase): HLModelToBackendMapper[NormalMode, GeneralInfoModel] with
    override type Out = InfoCreator
    override type Out2 = Info


    override def provideValidationContext(models: GeneralInfoModel*)(using context: GlobalValidationContext[? <: HLModel])(using Ox): Unit =
      models.foreach: model =>
        model.itsaID.foreach: itsaID =>
          val result = backend.findFirstByItsaID(itsaID)
          if result.isRight then
            context += ValidationIssue(
              source = "General Info Model",
              message = s"Entity with '$itsaID' already exists",
              isSoftWarning = true
            )
          backend.findBySerialNumber(model.serial).map: found =>
            if found.itsaID != itsaID then
              context += ValidationIssue(
                source = "General Info Model",
                message = s"Entity '${found.itsaID}' with serial number '${model.serial}' already exists",
                isSoftWarning = false
              )


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
        val hasWipingLog = backend.findBySerialNumber(model.serial).isRight
        val hddIDAlreadyExists = backend.findAllByItsaID(model.itsaID).isRight
        if hddIDAlreadyExists then
          context += ValidationIssue(
            source = "Hard Drive Model",
            message = s"Hard drive '${model.serial}' with itsaID ${model.itsaID} already exists.",
            isSoftWarning = true
          )
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
        val hasWipingLogs = notInComputer.nonEmpty
        backend.deleteByAllSerials(notInComputer.map(_.serial) *)//.left.map(HLDatabaseServiceException()).ok()
        val result = models.map: model =>
          backend.insert((model.copy(itsaID = itsaID) |> toCreator.convert).ok())
          model

        val suffixedModels: Seq[HardDriveModel] = wipingToWipingCreator.addLetterSuffixes(result*).ok()
        if !hasWipingLogs then
          suffixedModels.map(wipingToWipingCreator.toCreator.convert).map(_.map(backend.insert).ok())
          suffixedModels
        else
          suffixedModels.map(toCreator.convert).map(_.map(backend.insert).ok())
          suffixedModels
          
  



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
              isSoftWarning = false
            )


    end provideValidationContext


    private final val letters = 'A' to 'Z'
    private final val lettersInTheAlphabet = 26

    private[database] def addLetterSuffixes(models: HardDriveModel*): Either[HLDatabaseServiceExceptionBase, Seq[HardDriveModel]] =

      val result =
        ox.either:
          val disks: Seq[WipingCreator] = models.map(toCreator.convert).map(_.ok())
          if disks.isEmpty then
            Seq.empty[WipingCreator]
          else
            val baseId = disks.head.getItsaID
            val foundDrives: Seq[WipingCreator] = backend.findAllStartingWithItsaID[WipingCreator](baseId).map(_.map(WipingCreator.apply)) ?? Seq.empty[WipingCreator]
            val startIndex = foundDrives.size

            def toLetters(n: Int): String =
              if n <= 0 then ""
              else
                var x = n
                val sb = new StringBuilder
                
                while x > 0 do
                  x -= 1
                  sb.insert(0, letters(x % lettersInTheAlphabet))
                  x /= lettersInTheAlphabet
                sb.toString

            val newDisks = disks.zipWithIndex.map: (d, i) =>
              val idx = startIndex + i
              val suffix = toLetters(idx)
              d.copy(hddID = baseId + suffix)

            newDisks
      end result

      result.map: disks =>
        models.zip(disks).map: (model, disk) =>
          model.copy(itsaID = disk.hddID)


    end addLetterSuffixes


    override def executeSyncRules(models: HardDriveModel*)(using Ox): Result[Seq[HardDriveModel]] =
      val itsaID = requiresReadingItsaID.itsaID
      ox.either:
        backend.updateAndInsertAll[WipingCreator](models.map(toCreator.convert).map(_.ok()) *).left.map(HLDatabaseServiceException())
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


