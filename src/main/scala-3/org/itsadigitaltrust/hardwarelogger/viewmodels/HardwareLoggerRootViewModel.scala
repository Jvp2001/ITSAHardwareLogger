package org.itsadigitaltrust.hardwarelogger.viewmodels


import javafx.concurrent
import org.itsadigitaltrust.common.Operators.{??, toOption}
import org.itsadigitaltrust.common.forkJoin
import org.itsadigitaltrust.common.macros.NameOf
import org.itsadigitaltrust.common.processes.tlp.TlpStat
import org.itsadigitaltrust.hardwarelogger.core.HWLoggerShowMessage
import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.dialogs.DetailedValidationAlert
import org.itsadigitaltrust.hardwarelogger.models.HLModel
import org.itsadigitaltrust.hardwarelogger.models.HLModel.GeneralInfoModel
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{BackendValidationEvent, NotificationCentre, SaveToDBEvent}
import org.itsadigitaltrust.hardwarelogger.services.validation.{GlobalValidationContext, HLDBValidationError, ValidationIssue}
import org.itsadigitaltrust.hardwarelogger.services.{HLDatabaseService, HardwareGrabberService, HardwareIDValidationService, IDParser}
import org.itsadigitaltrust.hardwarelogger.shared.HardwareLoggerSharedData
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState
import org.itsadigitaltrust.hardwarelogger.tasks.{HardwareLoggerBusyWorker, HardwareLoggerSimpleTask}
import org.scalafx.extras
import org.scalafx.extras.ShowMessageLogger
import ox.OxError
import ox.either.ok
import scalafx.application.Platform
import scalafx.beans.property.*
import scalafx.event.ActionEvent
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.{Alert, ButtonType}
import scalafx.scene.input.MouseEvent
import scalafx.stage.Window

import scala.concurrent.duration.DurationInt

final class HardwareLoggerRootViewModel(using hardwareGrabberService: HardwareGrabberService, hardwareIDValidationService: HardwareIDValidationService, databaseService: HLDatabaseService, applicationState: HardwareLoggerApplicationState)
  extends RootViewModel, HWLoggerShowMessage:

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
  val isInNormalMode: BooleanProperty = BooleanProperty(ProgramMode.isInNormalMode)
  val shouldCaretBeAtEnd: BooleanProperty = BooleanProperty(false)
  val idStringProperty: StringProperty = StringProperty("")

  
  val hasABattery = TlpStat.hasBattery
  private var wasDuplicateIDWarningAlreadyShown = false
  private var isSaving = false

  idStringProperty() = applicationState.itsaID ?? ""

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

  private val reloadTask = new HardwareLoggerSimpleTask[Throwable, Unit](1)(using this):
    override def onCall()(using context: OxError[Throwable, [X] =>> Either[Throwable, X]]): Either[Throwable, Unit] =
      this.message() = "Reloading data..."
      NotificationCentre.reload().forkJoin()
      this.incrementProgress()
      Right(())

    override def onFinished(either: Either[Throwable, Unit]): Unit =
      if isSaving && either.isRight then
        isSaving = false
        Platform.runLater:
          new Alert(AlertType.Information):
            title = "Save Successful"
            headerText = "Data saved successfully!"
            contentText = "The data has been saved to the database successfully."
            showAndWait()

  end reloadTask



  Platform.Preferences

  private def performReload(): Unit =
    new HardwareLoggerBusyWorker("Reload", None)
    .doTask[Either[Throwable, Unit]]("Reloading data..."):
      reloadTask

    taskMessageProperty.unbind()
    taskMessageProperty.value = "Reload successful!"


  end performReload

  def setIDToMatchValueInDB(): Unit =
    ox.either:
      if isInNormalMode.value then
        idStringProperty.value = applicationState.generalInfo.map(_.head).ok().itsaID ?? ""
      else
        idStringProperty.value = applicationState.hardDrives.ok().headOption.ok().itsaID ?? ""
      shouldCaretBeAtEnd.value = idStringProperty.value.nonEmpty
    .showIfFailed()

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
                NotificationCentre.reload().forkJoin()
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
    isInNormalMode.value = mode == "Normal"

  def save(): Unit =
    given context: GlobalValidationContext[? <: HLModel] = GlobalValidationContext.globalValidationContext

    NotificationCentre.sendItsaIDChangedNotification(idStringProperty.get())
    // run the save task and wait for result so it actually executes
    new HardwareLoggerBusyWorker("Saving", None)
      .doTask[Either[Throwable, Unit]]("Saving data..."):
        new HardwareLoggerSimpleTask[Throwable, Unit](2)(using this):
          override def onCall()(using context: OxError[Throwable, [X] =>> Either[Throwable, X]]): Either[Throwable, Unit] =
            given task: HardwareLoggerSimpleTask[Throwable, Unit] = this
            Right(performGlobalSave())

          override def onFinished(either: Either[Throwable, Unit]): Unit =
            taskMessageProperty.unbind()
            taskMessageProperty() = "Save and reloaded data successfully!"



  end save

  private def performGlobalSave()(using context: OxError[Throwable, [X] =>> Either[Throwable, X]])(using task: HardwareLoggerSimpleTask[Throwable, Unit]): Unit =
    NotificationCentre.getGlobalValidationContext.clear()
    isSaving = true
    given context: GlobalValidationContext[? <: HLModel] = NotificationCentre.getGlobalValidationContext

    def performSave(): Unit =
      task.message() = "Saving data to database..."
      ox.forkUserError:
        ox.either.catchAll:
          NotificationCentre.executeSaveToDBEvent()
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
    var continueToSave = true
    if hardErrors.nonEmpty then
      // show hard validation issues on the FX thread and do NOT proceed with saving
      extras.onFX:
        handleHardValidationIssues(hardErrors)
    else if softWarnings.nonEmpty then
      val shouldSave = extras.onFXAndWait:
        handleSoftValidationIssues(softWarnings)
      if shouldSave then
        performSave().forkJoin()
    else
      performSave().forkJoin()

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
      val result = hardwareIDValidationService.validate(currentID.strip())
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


end HardwareLoggerRootViewModel

