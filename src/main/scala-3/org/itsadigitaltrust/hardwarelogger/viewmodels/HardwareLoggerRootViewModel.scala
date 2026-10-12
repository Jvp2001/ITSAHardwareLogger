package org.itsadigitaltrust.hardwarelogger.viewmodels


import javafx.beans.value.ObservableBooleanValue
import javafx.concurrent
import org.itsadigitaltrust.common.Operators.{??, toOption}
import org.itsadigitaltrust.common.forkJoin
import org.itsadigitaltrust.common.macros.NameOf
import org.itsadigitaltrust.common.processes.tlp.TlpStat
import org.itsadigitaltrust.hardwarelogger.core.HWLoggerShowMessage
import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.models.HLModel
import org.itsadigitaltrust.hardwarelogger.models.HLModel.{GeneralInfoModel, HardDriveModel}
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{BackendValidationEvent, HardDriveIDsChangedEvent, NotificationCentre, ProgramModeChangeNotificationListener, ReloadViewModelsEvent, SaveToDBEvent, SystemEvent}
import org.itsadigitaltrust.hardwarelogger.services.validation.{GlobalValidationContext, HLDBValidationError, ValidationIssue}
import org.itsadigitaltrust.hardwarelogger.services.{HLDatabaseService, HardwareGrabberService, HardwareIDValidationService, IDParser, Issue, IssueReporterService}
import org.itsadigitaltrust.hardwarelogger.shared.HardwareLoggerSharedData
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState
import org.itsadigitaltrust.hardwarelogger.tasks.{HardwareLoggerBusyWorker, HardwareLoggerSimpleTask}
import org.itsadigitaltrust.hardwarelogger.ui.derivation.UIGeneration
import org.itsadigitaltrust.hardwarelogger.ui.dialogs.DetailedValidationAlert
import org.scalafx.extras
import org.scalafx.extras.ShowMessageLogger
import ox.OxError
import ox.either.ok
import scalafx.application.Platform
import scalafx.beans.binding.{Bindings, BooleanBinding}
import scalafx.beans.property.*
import scalafx.event.ActionEvent
import scalafx.scene.input.MouseEvent
import scalafx.stage.Window

import scala.concurrent.duration.DurationInt

final class HardwareLoggerRootViewModel(using hardwareGrabberService: HardwareGrabberService, hardwareIDValidationService: HardwareIDValidationService, databaseService: HLDatabaseService, applicationState: HardwareLoggerApplicationState, issueReporterService: IssueReporterService)
  extends RootViewModel, HWLoggerShowMessage, ProgramModeChangeNotificationListener:

  lazy val identifier: String = NameOf[HardwareLoggerRootViewModel]
  lazy val idValidatorSchedule = new javafx.concurrent.ScheduledService[Unit]:
    override def createTask(): concurrent.Task[Unit] = () =>

      hardwareIDValidationService.validate(idStringProperty.get()) match
        case Left(error) => idErrorStringProperty.value = error.toString
        case Right(value) => idErrorStringProperty.value = ""
      ()
  end idValidatorSchedule

  val idFieldFocusProperty: BooleanProperty = BooleanProperty(false)
  val validIDProperty: BooleanProperty = BooleanProperty(false)
  val idErrorStringProperty: StringProperty = StringProperty("")
  val isInNormalMode: BooleanProperty = ProgramMode.isModeNormal
  val shouldCaretBeAtEnd: BooleanProperty = BooleanProperty(false)
  val idStringProperty: StringProperty = StringProperty("")

  import scalafx.Includes.*
  private val isAllHardDriveIDsValid: BooleanProperty = BooleanProperty(false)
  val isSaveButtonEnabled: BooleanProperty = BooleanProperty(false)
  isSaveButtonEnabled <== validIDProperty && disableNodeWhileTaskRunningProperty

  val hasABattery = TlpStat.hasBattery
  private var wasDuplicateIDWarningAlreadyShown = false

  idStringProperty() = applicationState.itsaID ?? ""

  NotificationCentre.subscribe[ReloadViewModelsEvent.type]: _ =>
    Platform.runLater:
      setIDToMatchValueInDB()
      if !isInNormalMode.value then
        NotificationCentre.publish(HardDriveIDsChangedEvent)

  given sharedData: Option[HardwareLoggerSharedData] = NotificationCentre.getFetchedData

  //
  given optionTaskMessageProperty: Option[StringProperty] = Option(taskMessageProperty)

  given optionTaskProgressProperty: Option[DoubleProperty] = Option(taskProgressProperty)

  given optionDisabledNodesWhileTaskIsRunning: Option[BooleanProperty] = Option(disableNodeWhileTaskRunningProperty)

  given itsaID: String = idStringProperty.value

  idStringProperty.onChange: (observable, oldValue, newValue) =>
    if newValue.isEmpty || newValue == null then
      idErrorStringProperty.value = "ID must not be empty!"
      validIDProperty.value = false
    else
      validateID()

  def reload(): Unit =
    performReload()
  end reload

  override protected def programModeChanged(mode: ProgramMode): Either[Error, Unit] =
    mode match
      case "Normal" =>
        isSaveButtonEnabled.unbind()
        isSaveButtonEnabled <== validIDProperty && !disableNodeWhileTaskRunningProperty
      case "HardDrive" =>
        isSaveButtonEnabled.unbind()
        isSaveButtonEnabled <== isAllHardDriveIDsValid && !disableNodeWhileTaskRunningProperty
        NotificationCentre.publish(HardDriveIDsChangedEvent)
      case _ => ()
    Right(())

  private final val saveTask =
      new HardwareLoggerSimpleTask[Throwable, Unit](2)(using this):
        override def onCall()(using context: OxError[Throwable, [X] =>> Either[Throwable, X]]): Either[Throwable, Unit] =
          given task: HardwareLoggerSimpleTask[Throwable, Unit] = this
          Right(performGlobalSave())

        override def onFinished(either: Either[Throwable, Unit]): Unit =
          taskMessageProperty.unbind()
          taskMessageProperty() = "Save process completed."

  private final val reloadTask = new HardwareLoggerSimpleTask[Throwable, Unit](1)(using this):
    override def onCall()(using context: OxError[Throwable, [X] =>> Either[Throwable, X]]): Either[Throwable, Unit] =
      this.message() = "Reloading data..."
      NotificationCentre.startReload(this)
      this.incrementProgress()
      Right(())

  end reloadTask


  
  private def performReload(): Unit =
    new HardwareLoggerBusyWorker("Reload", None)
    .doTask[Either[Throwable, Unit]]("Reloading data..."):
      reloadTask

    taskMessageProperty.unbind()
    taskMessageProperty.value = "Reload successful!"


  end performReload


  def setIDToMatchValueInDB(): Unit =
    val loggedComputerID =
      applicationState.generalInfo.flatMap(_.headOption).flatMap(_.itsaID)
        .orElse(applicationState.itsaID)
        .filter(HardDriveModel.hasLoggedItsaID)
    val hardDriveID =
      if isInNormalMode.value then None
      else applicationState.hardDrives.flatMap(_.headOption).flatMap(_.itsaID)
        .filter(HardDriveModel.hasLoggedItsaID)
    val matchingID = loggedComputerID.orElse(hardDriveID)
    matchingID.foreach: id =>
      idStringProperty.value = id
      shouldCaretBeAtEnd.value = true

  given toOptionConv[T]: Conversion[T, Option[T]] = Option.apply

  def reconnect(): Unit =
    performReconnect()
  end reconnect

  private def performReconnect(): Unit =
    new HardwareLoggerBusyWorker("Reload", None)
      .doTask[Either[Throwable, Unit]]("Reloading data..."):
        new HardwareLoggerSimpleTask[Throwable, Unit](1)(using this):
          override def onCall()(using context: OxError[Throwable, [X] =>> Either[Throwable, X]]): Either[Throwable, Unit] =
              ox.either:
                this.message() = "Reconnecting..."
                databaseService.connect().forkJoin().ok()
                this.message() = "Reloading data..."
                NotificationCentre.startReload(this)
                this.incrementProgress()

          override def onFinished(either: Either[Throwable, Unit]): Unit =
            taskMessageProperty.unbind()
            taskMessageProperty() = "Successfully reconnected to database!"


  end performReconnect

  private inline def tempDisableNodes[R](body: => R): R =
    disableNodeWhileTaskRunningProperty() = true
    val result = body
    disableNodeWhileTaskRunningProperty() = false
    result

  def switchMode(mode: ProgramMode): Unit =
    ProgramMode.mode = mode

  def save(): Unit =
    given context: GlobalValidationContext[? <: HLModel] = GlobalValidationContext.globalValidationContext

    NotificationCentre.sendItsaIDChangedNotification(idStringProperty.get())
    // run the save task and wait for result so it actually executes

    val hasValidID =
      if isInNormalMode.value then validIDProperty()
      else isAllHardDriveIDsValid()

    if hasValidID then
      new HardwareLoggerBusyWorker("Save").doTask("Saving")(saveTask)



  end save

  private def performGlobalSave()(using context: OxError[Throwable, [X] =>> Either[Throwable, X]])(using task: HardwareLoggerSimpleTask[Throwable, Unit]): Unit =
    NotificationCentre.getGlobalValidationContext.clear()
    var saveCompleted = false
    given context: GlobalValidationContext[? <: HLModel] = NotificationCentre.getGlobalValidationContext

    def performSave(): Unit =
      task.message() = "Saving data to database..."
      ox.forkUserError:
        ox.either:
          NotificationCentre.executeSaveToDBEvent()
          ()
      .join()
      task.incrementProgress()
    end performSave

    NotificationCentre.getFetchedData.foreach: data =>
      ox.fork:
        task.message() = "Executing backend validation..."
        logger.info(s"Executing backend validation...")
        NotificationCentre.executeBackendValidation()
      .join()
      task.incrementProgress()
    val allIssues = context.getIssues
    val hardErrors = allIssues.filterNot(_.isSoftWarning)
    val softWarnings = allIssues.filter(_.isSoftWarning)

    logger.info(s"Validation issues: ${allIssues.size} hard: ${hardErrors.size} soft: ${softWarnings.size}")
    if hardErrors.nonEmpty then
      // show hard validation issues on the FX thread and do NOT proceed with saving
      extras.onFX:
        handleHardValidationIssues(hardErrors)
    else if softWarnings.nonEmpty then
      val shouldSave = extras.onFXAndWait:
        handleSoftValidationIssues(softWarnings)
      if shouldSave then
        performSave().forkJoin()
        saveCompleted = true
    else
      performSave().forkJoin()
      saveCompleted = true

    if saveCompleted then
      reloadTask.call().forkJoin()

  end performGlobalSave

  private def handleHardValidationIssues(issues: List[ValidationIssue]): Unit =
    DetailedValidationAlert.showAsError(issues)


  private def handleSoftValidationIssues(issues: List[ValidationIssue]): Boolean =
    DetailedValidationAlert.showAsWarning(issues)
  end handleSoftValidationIssues

  def duplicateIDWasFound(itsaID: String): Unit =

    idStringProperty.value = itsaID
  end duplicateIDWasFound

  override protected def messageLogger: Option[ShowMessageLogger] = this.toOption

  private def validateID(showAlert: Boolean = false): Boolean =
    ox.either:
      val currentID: String = Option(idStringProperty.get()).getOrElse("")
      if currentID == "" then
        idErrorStringProperty.set("ID cannot be empty!")
        return false
      val result = hardwareIDValidationService(currentID.strip())
      result match
        case Left(value) =>
          validIDProperty.value = false
          idErrorStringProperty.value = value.toString
          if idErrorStringProperty.value == IDParser.ParserError.MissingCheckDigit.toString && idErrorStringProperty.value.contains(".") then
            idErrorStringProperty.value = ""

        case Right(value) =>
          validIDProperty.value = true
          idErrorStringProperty.value = ""
    //          appf databaseService.doesPCRecordExist(info) then
    //    //             licationState.generalInfo.map(_.head).map: info =>
    //            i idErrorStringProperty.value = s"The database contains a PC with the same serial number '${info.serial}!"
    //            else
    //              idErrorStringProperty.value = ""
    //              _idStringProperty.value = value.toString

    validIDProperty.value

  end validateID


  NotificationCentre.subscribe[HardDriveIDsChangedEvent.type]: _ =>
    applicationState.hardDrives.map: drives =>
      isAllHardDriveIDsValid() = false
      if !drives.forall: drive =>
        HardDriveModel.hasLoggedItsaID(drive.itsaID) &&
          hardwareIDValidationService(drive.itsaID).isRight
      then
        idErrorStringProperty() = "Make sure all hard drives have valid IDs!"
      else
        idErrorStringProperty() = ""
        isAllHardDriveIDsValid() = true

  def reportBug(): Unit =
    import scalafx.Includes.*
    import org.itsadigitaltrust.hardwarelogger.ui.derivation.CustomDataTypeSchemaGivens.given
    val result = UIGeneration.createDialog[Issue.BugReport](Issue.BugReport("Bug Report", ""), "Report Bug").delegate.showAndWait()
    result.map:
       case Some(value) =>
         ox.either:
          issueReporterService.report(value).left.foreach(error => showError("Error", error))
       case None => () // Cancelled
       case _ => showError("Error", "An error occurred whilst reporting the bug!")

end HardwareLoggerRootViewModel
