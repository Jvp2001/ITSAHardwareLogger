package org.itsadigitaltrust.hardwarelogger.viewmodels


import org.itsadigitaltrust.common.Operators.{??, in}
import org.itsadigitaltrust.common.Result

import org.itsadigitaltrust.hardwarelogger.delegates.{ProgramMode, ProgramModeChangedDelegate}
import org.itsadigitaltrust.hardwarelogger.dialogs.Dialogs
import org.itsadigitaltrust.hardwarelogger.models.HardDriveModel
import org.itsadigitaltrust.hardwarelogger.services
import org.itsadigitaltrust.hardwarelogger.services.HardwareIDValidationService.ValidationError
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.NotificationName.*
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{NotificationName, NotificationUserInfo}
import org.itsadigitaltrust.hardwarelogger.services.{HardwareIDValidationService, IDParser, ServicesModule}
import org.itsadigitaltrust.hardwarelogger.ui.ToastHelper

import scalafx.beans.property.*
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.Alert.AlertType.{Information, Warning}
import scalafx.scene.control.{Alert, ButtonType}

import scala.util.boundary
final class HardwareLoggerRootViewModel extends ViewModel, ServicesModule, ProgramModeChangedDelegate:

  val idFieldFocusProperty: BooleanProperty = BooleanProperty(false)
  val validIDProperty: BooleanProperty = BooleanProperty(false)
  val idStringProperty: StringProperty = StringProperty("")
  val idErrorStringProperty: StringProperty = StringProperty("")
  val isInNormalMode: BooleanProperty = BooleanProperty(ProgramMode.isInNormalMode)

  val shouldCaretBeAtEnd: BooleanProperty = BooleanProperty(false)

  private var wasDuplicateIDWarningAlreadyShown = false
  private val idErrorAlert = new Alert(AlertType.Error, "", ButtonType.OK):
    contentText <== idErrorStringProperty

  given itsaID: String = idStringProperty.value

  hardwareIDValidationService.validate(idStringProperty.get)
  idStringProperty.onChange: (observable, oldValue, newValue) =>
    if newValue.isEmpty || newValue == null then
      idErrorStringProperty.value = "ID must not be empty!"
      validIDProperty.value = false
    else
      validateID()

  override def onReceivedNotification(message: Message): Unit =
    message.name match
      case NotificationName.DBSuccess => onDBSuccess(message)
      case NotificationName.FoundDuplicateRowsWithID => onDuplicateIDFound(message)
      case NotificationName.Reload => onReload(message)
      case NotificationName.ShowDuplicateDriveWarning => duplicateDrives(message)
      case NotificationName.ProgramModeChanged => onProgramModeChanged(ProgramMode.mode)
      case _ => ()

  private def onDBSuccess(message: Message): Unit = ()
//    ToastHelper.makeText(null, "Data has been saved!", 3000, 200, 200)
//    wasDuplicateIDWarningAlreadyShown = false

  private def duplicateDrives(message: Message) =

    val serial = message.userInfo("drives") match
      case arr: Array[String] => arr.toSeq
      case seq: Seq[String] => seq
      case s: String => Seq(s)
      case other => Seq.empty[String]

    new Alert(Warning, "Duplicate Drive Found!", ButtonType.Yes, ButtonType.No):
      contentText = s"A drive with the serial number '$serial' already exists. Do you want to continue?"
      showAndWait() match
        case Some(ButtonType.Yes) =>
          notificationCentre.post(ContinueWithDuplicateDrive)
        case _ =>
          reload()
  end duplicateDrives


  //TODO: Validate and fix the data actually going into the database.
  private def onDuplicateIDFound(message: Message) =
//    if !wasDuplicateIDWarningAlreadyShown then
    new Alert(AlertType.Warning, "Do you want to continue with saving this data? If so, the current data with the same ID will be marked as an error.", ButtonType.Yes, ButtonType.No):
      headerText = s"ID '${idStringProperty.value}' is already in use."
      showAndWait() match
        case Some(ButtonType.Yes) =>
          databaseService.markAllRowsWithIDInDBAsError(idStringProperty.value)
          val userInfo = NotificationUserInfo:
            val id = idStringProperty.value
          //    reload()
          notificationCentre.post(Save, None, Option(userInfo))
          wasDuplicateIDWarningAlreadyShown = true
        case _ =>
          wasDuplicateIDWarningAlreadyShown = false
//    end if

  private def onReload(message: Message): Unit =
    if ProgramMode.isInNormalMode then
      val pcInfo = hardwareGrabberService.generalInfo
      val info = pcInfo
      val itsaId = info.itsaID
      idStringProperty.value = itsaId ?? ""
      logger.info(s"Itsa ID: $itsaId")


  override def setup(): Unit =
    reload()
    setIDToMatchValueInDB()


  def reconnect(): Unit =
    databaseService.connectAsync():
      case Result.Success(_) =>
        logger.info("Database connection established successfully.")
      case Result.Error(err) =>
        logger.info(s"Database connection failed: $err")
        Dialogs.showDBConnectionError()
    if !databaseService.testConnection() then
      Dialogs.showDBConnectionError()

  end reconnect

  def switchMode(mode: ProgramMode): Unit =
    ProgramMode.mode = mode
    isInNormalMode.value = mode == "Normal"

  def save(): Unit =
    //HLTaskRunner.runLater("Save"): () =>
    validateID(true)
    if !validIDProperty.value then
      idFieldFocusProperty.value = true
      return
    if ProgramMode.isInNormalMode then
      val notWipedDrives = findNonWipedDrives()
      if notWipedDrives.nonEmpty then
        showDrivesNotWipedAlert(notWipedDrives)

    databaseService.itsaID = idStringProperty.value
    val userInfo = NotificationUserInfo:
      val id = idStringProperty.value
//    reload()
    notificationCentre.post(Save, None, Option(userInfo))

  end save


  private def validateID(showAlert: Boolean = false): Unit =
    val currentID: String = Option(idStringProperty.get()).getOrElse("")
    if currentID == "" then
      idErrorStringProperty.set("ID cannot be empty!")
      return

    val result = hardwareIDValidationService.validate(currentID.strip())
    result match
      case Result.Error(value) =>
        validIDProperty.value = false
        idErrorStringProperty.value = value.toString
        if idErrorStringProperty.value ==  IDParser.ParserError.MissingCheckDigit.toString && idErrorStringProperty.value.contains(".") then
          idErrorStringProperty.value = ""
        if showAlert then
          value match
            case ValidationError.ParserError(error) => idErrorStringProperty.value = error.toString
            case error@ValidationError.IncorrectCheckDigit(expected, got) =>

              idErrorAlert.showAndWait() match
                case _ => idFieldFocusProperty.value = true
      case Result.Success(value) =>
        validIDProperty.value = true
        //TODO: Check if you want to wipe the ID after saving.
        idErrorStringProperty.value = ""
        idStringProperty.value = value.toString
  end validateID

  def reload(): Unit =
    hardwareGrabberService.load(): () =>
      notificationCentre.post(Reload)
      setIDToMatchValueInDB()

  end reload

  def setIDToMatchValueInDB(): Unit =
    if isInNormalMode.value then
      idStringProperty.value = hardwareGrabberService.generalInfo.itsaID ?? ""

    else
      idStringProperty.value = hardwareGrabberService.hardDrives.headOption.map(_.itsaID) ?? ""
    shouldCaretBeAtEnd.value = idStringProperty.value.nonEmpty


  override def onProgramModeChanged(mode: ProgramMode): Unit =
    isInNormalMode.value = mode == "Normal"
    reload()


  private def findNonWipedDrives() =
    hardwareGrabberService.hardDrives.filterNot: hardDrive =>
      databaseService.findWipingRecord(hardDrive.serial).isDefined



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
          case Some(ButtonType.Yes) => databaseService.addWipingRecords(hardwareGrabberService.hardDrives *)
          case _ => ()
  end showDrivesNotWipedAlert

end HardwareLoggerRootViewModel
