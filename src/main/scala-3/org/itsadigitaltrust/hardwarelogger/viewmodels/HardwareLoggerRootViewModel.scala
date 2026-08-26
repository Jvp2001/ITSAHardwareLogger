    package org.itsadigitaltrust.hardwarelogger.viewmodels


import javafx.concurrent
import org.itsadigitaltrust.common.Operators.{??, toOption, |>}
import org.itsadigitaltrust.common.forkJoin
import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.common.macros.NameOf
import org.itsadigitaltrust.hardwarelogger.core.HWLoggerShowMessage
import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.dialogs.{Dialogs, ValidationWarningDialog}
import org.itsadigitaltrust.hardwarelogger.models.HLModel.{GeneralInfoModel, MediaModel, ProcessorModel}
import org.itsadigitaltrust.hardwarelogger.models.{HLModel, HardDriveModel}
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{BackendValidationEvent, NotificationCentre, TabPreparationEvent}
import org.itsadigitaltrust.hardwarelogger.services.validation.{GlobalValidationContext, HLDBValidationError, ValidationIssue}
import org.itsadigitaltrust.hardwarelogger.services.{ApplicationServicesRegistry, HLDatabaseService, HLDatabaseServiceException, HardwareGrabberService, HardwareIDValidationService, IDParser, OshiHardwareGrabberApplicationService}
import org.itsadigitaltrust.hardwarelogger.shared.HardwareLoggerSharedData
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState
import org.itsadigitaltrust.hardwarelogger.tasks.{CommonHardwareLoggerTasksAndWorkers, HardwareLoggerSimpleTask}
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.{GeneralInfoTableRowViewModel, MediaTableRowViewModel, MemoryTableRowViewModel, ProcessorTableRowViewModel}
import org.itsadigitaltrust.hardwarelogger.viewmodels.tabs.{HardDrivesTabViewModel, MemoryTabViewModel, TabTableViewModel}
import org.scalafx.extras
import org.scalafx.extras.{BusyWorker, ShowMessage, ShowMessageLogger, offFX, offFXAndWait, parentWindow, runTask}
import ox.{EitherMode, Fork}
import ox.either.{fail, ok}
import scalafx.application.Platform
import scalafx.beans.property.*
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.Alert.AlertType.Information
import scalafx.scene.control.{Alert, ButtonType, Dialog}
import scalafx.stage.Window

import java.util.concurrent.CountDownLatch
import scala.concurrent.duration.DurationInt
import scala.util.boundary

final class HardwareLoggerRootViewModel(using hardwareGrabberService: HardwareGrabberService, hardwareIDValidationService: HardwareIDValidationService, databaseService: HLDatabaseService, applicationState: HardwareLoggerApplicationState)
  extends RootViewModel, HWLoggerShowMessage:

  lazy val identifier: String = NameOf[HardwareLoggerRootViewModel]

  given sharedData: Option[HardwareLoggerSharedData] = NotificationCentre.getFetchedData
  override protected def messageLogger: Option[ShowMessageLogger] = this.toOption

  private val _idStringProperty: StringProperty = StringProperty("")
  val idStringProperty: StringProperty = _idStringProperty
  idStringProperty() = applicationState.itsaID ?? ""
  val idFieldFocusProperty: BooleanProperty = BooleanProperty(false)
  val validIDProperty: BooleanProperty = BooleanProperty(false)
  val idErrorStringProperty: StringProperty = StringProperty("")
  val isInNormalMode: BooleanProperty = BooleanProperty(ProgramMode.isInNormalMode)
  val shouldCaretBeAtEnd: BooleanProperty = BooleanProperty(false)
  private var wasDuplicateIDWarningAlreadyShown = false
  private lazy val idErrorAlert = new Alert(AlertType.Error, "", ButtonType.OK):
    contentText <== idErrorStringProperty

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




  private def performGlobalSave(): Unit =
    given context: GlobalValidationContext[HLModel] = GlobalValidationContext()

    Thread.startVirtualThread: () =>
      ox.supervised:

        val backendFork = ox.fork:
          NotificationCentre.getFetchedData.map: data =>
            data.info.flatMap(_.headOption).map: info =>
              databaseService.runValidation(info)
          val event = BackendValidationEvent()
          NotificationCentre.sendBackendValidationEvent(event)
          event.getForks.foreach(_.join())

        backendFork.join()
        val event = TabPreparationEvent()
        NotificationCentre.sendTabPreparationEvent(event)

        event.getForks.foreach(_.join())

        val allIssues = context.getIssues
        val hardErrors = allIssues.filterNot(_.isSoftWarning)
        val softWarnings = allIssues.filter(_.isSoftWarning)

        if hardErrors.nonEmpty then
          Platform.runLater:
            handleHardValidationIssues(hardErrors)
        else if softWarnings.nonEmpty then
          val countDownLatch = new CountDownLatch(1)
          handleSoftValidationIssues(softWarnings, countDownLatch)
        else
          NotificationCentre.saveDB()

  end performGlobalSave

  private def handleHardValidationIssues(issues: List[HLDBValidationError]): Unit = ()

  private def createSoftIssueConfirmationAlert(issue: ValidationIssue): Alert =
    val alert = new Alert(AlertType.Confirmation)
    alert.setTitle(issue.source)
    alert.setContentText(issue.message)
    alert

  private def handleSoftValidationIssues(issues: List[ValidationIssue], latch: CountDownLatch): Boolean =
    var userConfirmed = false
    Platform.runLater:
      val alert = new ValidationWarningDialog(issues)
      val result = alert.showAndWait()

      userConfirmed = result.isDefined && result.get == ButtonType.OK
      latch.countDown()

    latch.await()
    userConfirmed
  end handleSoftValidationIssues




  def setup(): Unit =
    reload()
    setIDToMatchValueInDB()


  given Conversion[() => Unit, Runnable] with
    override def apply(x: () => Unit): Runnable = x


  given toOptionConv[T]: Conversion[T, Option[T]] = Option.apply
  
  
  def reconnect(): Unit =
    CommonHardwareLoggerTasksAndWorkers.performReconnectWorker()
  end reconnect

  def switchMode(mode: ProgramMode): Unit =
    ProgramMode.mode = mode
    isInNormalMode.value = mode == "Normal"






  def save(): Unit =
    ox.either:
      import scala.languageFeature.implicitConversions
      //    given context: BusyWorkerContext.Prompt = BusyWorkerContext.Prompt(s"${idStringProperty.value} is already in use.", "Do you want to save the data?")
      NotificationCentre.sendItsaIDChangedNotification(itsaID)
      showConfirmationYesNoCancel("Save?", s"{{${idStringProperty.value}}} is already in use.", "Do you want to save the data?") match
        case Some(true) =>
          validateID(true)
          if !validIDProperty.value then
            idFieldFocusProperty.value = true
        case _ => false



  lazy val idValidatorSchedule = new javafx.concurrent.ScheduledService[Unit]:
    override def createTask(): concurrent.Task[Unit] = () =>

      hardwareIDValidationService.validate(idStringProperty.get()) match
        case Left(error) => idErrorStringProperty.value = error.toString
        case Right(value) => idErrorStringProperty.value = ""
      ()
  end idValidatorSchedule
  
  

  def duplicateIDWasFound(itsaID: String): Unit =

    _idStringProperty.value = itsaID
  end duplicateIDWasFound

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
          //TODO: Check if you want to wipe the ID after saving.
          applicationState.generalInfo.map(_.head).map: info =>
            if databaseService.doesPCRecordExist(info) then
              idErrorStringProperty.value = s"The database contains a PC with the same serial number '${info.serial}!"
            else
              idErrorStringProperty.value = ""
              _idStringProperty.value = value.toString

    validIDProperty.value
  end validateID

  def onProgramModeChanged(mode: ProgramMode)(using ox.Ox): Unit =
    NotificationCentre.programModeChanged(mode)

  def reload(): Unit =
    disableNodeWhileTaskRunningProperty() = true
    extras.offFX:
      CommonHardwareLoggerTasksAndWorkers.performReloadWorker()
  end reload


  def setIDToMatchValueInDB(): Unit =
    ox.either:
      if isInNormalMode.value then
        _idStringProperty.value = applicationState.generalInfo.map(_.head).ok().itsaID ?? ""
      else
        _idStringProperty.value = applicationState.hardDrives.ok().headOption.ok().itsaID ?? ""
      shouldCaretBeAtEnd.value = idStringProperty.value.nonEmpty
    .showIfFailed()

  def programModeChanged(mode: ProgramMode)(using ox.Ox): Unit =
    onProgramModeChanged(mode)







end HardwareLoggerRootViewModel

