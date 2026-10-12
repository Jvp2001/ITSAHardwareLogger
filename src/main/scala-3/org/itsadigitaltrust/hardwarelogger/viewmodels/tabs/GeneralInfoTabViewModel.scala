package org.itsadigitaltrust.hardwarelogger.viewmodels.tabs

import org.itsadigitaltrust.hardwarelogger.models.HLModel
import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.services.HardwareGrabberService
import org.itsadigitaltrust.hardwarelogger.services.database.HLDatabaseService
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{PerformBackendValidationListener, RequiresGlobalValidationContext}
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState
import org.itsadigitaltrust.hardwarelogger.viewmodels.ViewModel
import ox.Ox

class GeneralInfoTabViewModel(using hardwareGrabberService: HardwareGrabberService, databaseService: HLDatabaseService)(using applicationState: HardwareLoggerApplicationState) extends ViewModel, RequiresGlobalValidationContext, PerformBackendValidationListener:
  def info: Option[HLModel.GeneralInfoModel] =
    applicationState.generalInfo.map(_.head)

  override protected def onBackendValidationFork()(using Ox): Unit =
    if ProgramMode.isInNormalMode then
      databaseService.runValidation[HLModel.GeneralInfoModel](info.toSeq*)

  override protected def save()(using Ox): Either[Error, Unit] =
    if ProgramMode.isInHardDriveMode then
      Right(())
    else
      info match
        case Some(model) =>
          databaseService.executeFinalSave(model).left.map(hardwareGrabberService.LoadError(_))
        case None =>
          Left(hardwareGrabberService.LoadError("PC information is not loaded; cannot save it."))
