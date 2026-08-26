package org.itsadigitaltrust.hardwarelogger.services.database

import org.itsadigitaltrust.common.Operators.|>
import org.itsadigitaltrust.hardwarelogger.delegates.{HardDriveMode, NormalMode, ProgramMode, ProgramOperationMode}
import org.itsadigitaltrust.hardwarelogger.models.HLModel
import org.itsadigitaltrust.hardwarelogger.services.database.mappers.HLModelToBackendMapper

import scala.Conversion.into
import scala.compiletime.{deferred, summonInline}


trait HLDatabaseServiceCoordinatorOperationContext:
  given mode: ProgramMode = deferred

object HLDatabaseServiceCoordinatorOperationContextDefaults:
  given normalModeContext: HLDatabaseServiceCoordinatorOperationContext with
    override given mode: ProgramMode = "Normal"

  given hardDriveModeContext: HLDatabaseServiceCoordinatorOperationContext with
    override given mode: ProgramMode = "HardDrive"


private[database] object HLDatabaseServiceCoordinator:

  given programModeBasedContext: HLDatabaseServiceCoordinatorOperationContext with
    override given mode: ProgramMode =
      if ProgramMode.isInHardDriveMode then
        "HardDrive"
      else
        "Normal"
  end programModeBasedContext


  inline def executeWithContext[M <: HLModel, R](inline f: HLModelToBackendMapper[? <: ProgramOperationMode, M] ?=> R)(using context: HLDatabaseServiceCoordinatorOperationContext = programModeBasedContext): R =
    context.mode match
      case "Normal" => f(using summonInline[HLModelToBackendMapper[NormalMode, M]])
      case "HardDrive" => f(using summonInline[HLModelToBackendMapper[HardDriveMode, M]])




  inline def runWithCorrectMapper[M <: HLModel, R](model: M)(
                                           onNormal: HLModelToBackendMapper[NormalMode, M] ?=> R,
                                           onHardDrive: HLModelToBackendMapper[HardDriveMode, M] ?=> R
  )(using context: HLDatabaseServiceCoordinatorOperationContext = programModeBasedContext): R =
    context.mode match
      case "Normal" =>
        onNormal(using summonInline[HLModelToBackendMapper[NormalMode, M]])

      case "HardDrive" =>
        onHardDrive(using summonInline[HLModelToBackendMapper[HardDriveMode, M]])


end HLDatabaseServiceCoordinator