package org.itsadigitaltrust.hardwarelogger.services.database

import org.itsadigitaltrust.common.Operators.??
import org.itsadigitaltrust.hardwarelogger.backend.database.{EntityCreatorToEntityMapper, HLDatabase}
import org.itsadigitaltrust.hardwarelogger.delegates.ProgramOperationMode
import org.itsadigitaltrust.hardwarelogger.models.HLModel
import org.itsadigitaltrust.hardwarelogger.services.database.mappers.HLModelToBackendMapper
import org.itsadigitaltrust.hardwarelogger.services.validation.GlobalValidationContext
import ox.Ox
import ox.either.{fail, ok}

/**
 * Ox-Native Database Lifecycle Trait.
 * Integrates 'using Ox' capabilities so operations run natively inside
 * structured concurrency scopes on Project Loom virtual threads.
 */
private[database] trait DBOperationLifecycle[M <: HLModel]:

  type Phase = "BEFORE_SAVE" | "AFTER_SAVE"
  
  /**
   * Phase 1: Pre-Transaction Validation Analysis.
   * Runs under an active Ox context, enabling parallel database checks if needed.
   */
  def provideValidationContext(models: M*)( using context: GlobalValidationContext[? <: HLModel])(using Ox): Unit

  /**
   * Phase 2: Core Table State Diagnostic Check.
   * Invoked immediately before and after writing mutations to log row metrics.
   */
  def queryTableState(models: Seq[M], phase: Phase)(using Ox): Unit = ()

  /**
   * Phase 3: Synchronous DB Mutation Target.
   * Executes explicit row additions or structural deletions.
   */
  def executeSyncRules(models: M*)(using Ox): HLDatabaseOperations.Result[Seq[M]]

  /**
   * High-level template orchestrating the database execution lifecycle.
   * Executes sequentially within the forked supervisor thread context.
   */
  final def executeSave(models: M*)(using Ox): HLDatabaseOperations.Result[Seq[M]] =
    ox.either:
      // 1. Audit the initial state before committing mutations
      queryTableState(models, "BEFORE_SAVE")
  
      // 2. Perform table synchronization transformations (Creates / Deletes)
      val synchronisedModels = executeSyncRules(models*).ok()
  
      // 3. Re-verify table rows match expected results following mutation completion
      queryTableState(synchronisedModels, "AFTER_SAVE")
  
      synchronisedModels

  end executeSave
end DBOperationLifecycle


trait HLDatabaseOperations[ModeType <: ProgramOperationMode, Model <: HLModel](using backend: HLDatabase):
  self: HLDBOperationLifecycle[ModeType, Model] & HLModelToBackendMapper[ModeType, Model] =>

  import org.itsadigitaltrust.hardwarelogger.services.database.mappers.EntityCreatorModelConversions.given
  import HLDatabaseOperations.*
  def findByItsaID(itsaID: String): Result[Model] =
    ox.either:
      backend.findFirstByItsaID(itsaID)
        .left.map(HLDatabaseServiceException(itsaID))
        .map(self.toModel.convert)
      ?? HLDatabaseServiceException.noEntriesFound(itsaID).fail()

  def findBySerial(serial: String): Result[Model] =
    ox.either:
      backend.findBySerialNumber(serial)
        .left.map(HLDatabaseServiceException(serial))
        .map(self.toModel.convert)
      ?? HLDatabaseServiceException.noEntriesFound(serial).fail()


end HLDatabaseOperations





trait HLDBOperationLifecycle[ModeType <: ProgramOperationMode, M <: HLModel](using backend: HLDatabase) extends DBOperationLifecycle[M], HLDatabaseOperations[ModeType, M]:
  self: HLModelToBackendMapper[ModeType, M] =>

  

object HLDatabaseOperations:
  type Result[R] = Either[HLDatabaseServiceExceptionBase, R]


