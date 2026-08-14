    package org.itsadigitaltrust.hardwarelogger.viewmodels


import javafx.concurrent
import org.itsadigitaltrust.common.Operators.{??, toOption, |>}
import org.itsadigitaltrust.common.forkJoin
import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.hardwarelogger.core.HWLoggerShowMessage
import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.dialogs.Dialogs
import org.itsadigitaltrust.hardwarelogger.models.HLModel.{GeneralInfoModel, MediaModel, ProcessorModel}
import org.itsadigitaltrust.hardwarelogger.models.HardDriveModel
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.NotificationCentre
import org.itsadigitaltrust.hardwarelogger.services.{ApplicationServicesRegistry, HLDatabaseService, HLDatabaseServiceException, HardwareGrabberService, HardwareIDValidationService, IDParser, OshiHardwareGrabberApplicationService}
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState
import org.itsadigitaltrust.hardwarelogger.tasks.{CommonHardwareLoggerTasksAndWorkers, HardwareLoggerSimpleTask}
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.{GeneralInfoTableRowViewModel, MediaTableRowViewModel, MemoryTableRowViewModel, ProcessorTableRowViewModel}
import org.itsadigitaltrust.hardwarelogger.viewmodels.tabs.{HardDrivesTabViewModel, MemoryTabViewModel, TabTableViewModel}
import org.scalafx.extras
import org.scalafx.extras.{BusyWorker, ShowMessage, ShowMessageLogger, offFX, offFXAndWait, parentWindow, runTask}
import ox.EitherMode
import ox.either.{fail, ok}
import scalafx.beans.property.*
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.Alert.AlertType.Information
import scalafx.scene.control.{Alert, ButtonType}
import scalafx.stage.Window

import scala.concurrent.duration.DurationInt
import scala.util.boundary

final class HardwareLoggerRootViewModel(using hardwareGrabberService: HardwareGrabberService, hardwareIDValidationService: HardwareIDValidationService, databaseService: HLDatabaseService, applicationState: HardwareLoggerApplicationState)
  extends RootViewModel, HWLoggerShowMessage:


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




  def continueWithDuplicateDrives(drives: Seq[String]): Unit =
    val alertContentText =
      if drives.length == 1 then
        s"A drive with the serial number '${drives.head} already exists. Do you want to continue?"
      else
        s"Drives with the serial numbers '${drives.mkString(", ")} already exist. Do you want to continue?"
    end alertContentText

    if showConfirmation("Serial Number is duplicated", "Continue With Duplicate Drives?", alertContentText) then
      reload()

    else
      ()

  end continueWithDuplicateDrives

  
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


      databaseService.itsaID = idStringProperty.value


      showConfirmationYesNoCancel("Save?", s"{{${idStringProperty.value}}} is already in use.", "Do you want to save the data?") match
        case Some(true) =>
          validateID(true)
          if !validIDProperty.value then
            idFieldFocusProperty.value = true

          if ProgramMode.isInNormalMode then
            val notWipedDrives = findNonWipedDrives().ok()
            if notWipedDrives.nonEmpty then
              showDrivesNotWipedAlert(notWipedDrives)
  def dbSuccess(): Unit = ()
//    NotificationCentre.on
//
//    val task = new RootViewModelBatchedTask(5000.milliseconds, list)
//    val result = new RootViewModelBusyWorker("Saving to Database", disabledNodes = disabledNodes.toSeq)
//      .doTask(task)
//
//    reload()
//    extras.onFX:
//      showInformation("Database Connection Successful", "Successfully connected to the database.", "")
//pedDrives)
//
//          reload()
//        case _ => ()
//
//    .showIfFailed()
//
//
//  end save


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

  private def validateID(showAlert: Boolean = false): Unit =
    ox.either:
      val currentID: String = Option(idStringProperty.get()).getOrElse("")
      if currentID == "" then
        idErrorStringProperty.set("ID cannot be empty!")
        return
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
  end validateID

  def onProgramModeChanged(mode: ProgramMode): Unit =
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

  def programModeChanged(mode: ProgramMode): Unit =
    onProgramModeChanged(mode)



  private def findNonWipedDrives() =
    ox.either:
      applicationState.hardDrives.ok()
        .filterNot: hardDrive =>
          databaseService.findWipingRecord(hardDrive.serial).isLeft


  private def showDrivesNotWipedAlert(notWipedDrives: Seq[HardDriveModel]) =
    boundary:

      var serials =
        if ProgramMode.isInNormalMode then
          notWipedDrives.map(_.serial).mkString(", ")
        else
          notWipedDrives.map(_.itsaID).mkString(", ")
      end serials
      serials = serials.patch(serials.lastIndexOf(", "), " & ", 0)
      serials = if serials.split(", ").length == 2 then serials.replaceFirst(", ", "") else serials
      serials = if serials.trim.startsWith("& ") then serials.replaceFirst("& ", "") else serials

      val word = serials.length match
        case 0 => boundary.break()
        case 1 => "drive"
        case _ => "drives"


      new Alert(Information, s"The $word '$serials' have not been logged. Do you want to log them now?", ButtonType.Yes, ButtonType.No):
        headerText = "No Wiping Records Found"
        showAndWait() match
          case Some(ButtonType.Yes) => databaseService.addWipingRecords(itsaID, applicationState.hardDrives ?? Seq.empty[HardDriveModel] *)
          case _ => ()
  end showDrivesNotWipedAlert

end HardwareLoggerRootViewModel

