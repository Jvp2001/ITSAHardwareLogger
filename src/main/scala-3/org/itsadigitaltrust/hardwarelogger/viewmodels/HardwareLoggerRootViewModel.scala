package org.itsadigitaltrust.hardwarelogger.viewmodels


import org.itsadigitaltrust.common
import common.Operators.??
import org.itsadigitaltrust.common.optional.?
import org.itsadigitaltrust.common.{Result, optional}
import org.itsadigitaltrust.hardwarelogger.HardwareLoggerApplication.{databaseService, getClass}
import org.itsadigitaltrust.hardwarelogger.delegates.{ProgramMode, ProgramModeChangedDelegate}
import org.itsadigitaltrust.hardwarelogger.models.HardDriveModel
import org.itsadigitaltrust.hardwarelogger.services.HardwareIDValidationService.ValidationError
import org.itsadigitaltrust.hardwarelogger.services.NotificationChannel.{ContinueWithDuplicateDrive, DBSuccess, Reload, Save, ShowDuplicateDriveWarning}
import scalafx.beans.property.*
import org.itsadigitaltrust.hardwarelogger.services.{HardwareIDValidationService, NotificationCentre, NotificationChannel, ServicesModule}
import scalafx.application.Platform
import scalafx.beans.binding.Bindings
import scalafx.scene.control.{Alert, ButtonType}
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.Alert.AlertType.{Information, Warning}

import scala.util.boundary


final class HardwareLoggerRootViewModel extends ViewModel with ServicesModule with ProgramModeChangedDelegate:

  val idFieldFocusProperty: BooleanProperty = BooleanProperty(false)
  val validIDProperty: BooleanProperty = BooleanProperty(false)
  val idStringProperty: StringProperty = StringProperty("")
  val idErrorStringProperty: StringProperty = StringProperty("")
  val isInNormalMode: BooleanProperty = BooleanProperty(ProgramMode.isInNormalMode)


  private var wasDuplicateIDWarningAlreadyShown = false
  private val idErrorAlert = new Alert(AlertType.Error, "", ButtonType.OK):
    contentText <== idErrorStringProperty



  hardwareIDValidationService.validate(idStringProperty.get)
  idStringProperty.onChange: (observable, oldValue, newValue) =>
    if newValue.isEmpty || newValue == null then
      idErrorStringProperty.value = "ID must not be empty!"
    else
      validateID()

  notificationCentre.subscribe(DBSuccess): (key, _) =>
    new Alert(Information, "Data has been saved!", ButtonType.OK).showAndWait()
    wasDuplicateIDWarningAlreadyShown = false

  notificationCentre.subscribe(ShowDuplicateDriveWarning): (key, args: Seq[Any]) =>
    val serial = args.head.asInstanceOf[String]
    new Alert(Warning, "Duplicate Drive Found!", ButtonType.Yes, ButtonType.No):
      contentText = s"A drive with the serial number '$serial' already exists. Do you want to continue?"
      showAndWait() match
        case Some(ButtonType.Yes) =>
          notificationCentre.publish(ContinueWithDuplicateDrive)
        case _ =>
          reload()

  //TODO: Validate and fix the data actually going into the database.
  notificationCentre.subscribe(NotificationChannel.FoundDuplicateRowsWithID): (key, _) =>
    if !wasDuplicateIDWarningAlreadyShown then
      new Alert(AlertType.Warning, "Do you want to continue with saving this data? If so, the current data with the same ID will be marked as an error.", ButtonType.Yes, ButtonType.No):
        headerText = s"ID '$idStringProperty' is already in use."
        showAndWait() match
          case Some(ButtonType.Yes) =>
            databaseService.markAllRowsWithIDInDBAsError(idStringProperty.value)
            wasDuplicateIDWarningAlreadyShown = true
          case _ => ()
    end if

  notificationCentre.subscribe(NotificationChannel.Reload): (key, _) =>
    if ProgramMode.isInNormalMode then
      optional:
        val pcInfo = Option(hardwareGrabberService.generalInfo)
        val info = pcInfo.?
        val itsaId = info.itsaID.?
        idStringProperty.value = itsaId
        println(s"Itsa ID: $itsaId")
  
    
  override def setup(): Unit =
    reload()


  def switchMode(mode: ProgramMode): Unit =
    ProgramMode.mode = mode
    isInNormalMode.value = mode == "Normal"
  def save(): Unit =
    validateID(true)

    if ProgramMode.isInNormalMode then
      val notWipedDrives = findNonWipedDrives()
      if notWipedDrives.nonEmpty then
        showDrivesNotWipedAlert(notWipedDrives)
    notificationCentre.publish(Save)
  end save


  def validateID(showAlert: Boolean = false): Unit =
    val currentID: String = Option(idStringProperty.get()).getOrElse("")
    if currentID == "" then
      idErrorStringProperty.set("ID cannot be empty!")
      return

    val result = hardwareIDValidationService.validate(currentID.strip())
    result match
      case Result.Error(value) =>
        idErrorStringProperty.value = value.toString
        if showAlert then
          value match
            case ValidationError.ParserError(error) => idErrorStringProperty.value = error.toString
            case error@ValidationError.IncorrectCheckDigit(expected, got) =>

              idErrorAlert.showAndWait() match
                case _ => idFieldFocusProperty.value = true
      case Result.Success(value) =>
        idErrorStringProperty.value = ""
        idStringProperty.value = value.toString
  end validateID

  def reload(): Unit =
    hardwareGrabberService.load(): () =>
      notificationCentre.publish(NotificationChannel.Reload)
      if isInNormalMode.value then
        idStringProperty.value = hardwareGrabberService.generalInfo.itsaID ?? ""
  end reload

  override def onProgramModeChanged(mode: ProgramMode): Unit =
    isInNormalMode.value = mode == "Normal"
    reload()


  private def findNonWipedDrives() =
    hardwareGrabberService.hardDrives.filterNot: hardDrive =>
      databaseService.findWipingRecord(hardDrive.serial) match
        case Some(_) => true
        case None => false


  private def showDrivesNotWipedAlert(notWipedDrives: Seq[HardDriveModel]) =
    boundary:

      var serials =
        if ProgramMode.isInNormalMode then
          notWipedDrives.map(_.serial).mkString(", ")
        else
          notWipedDrives.map(_.itsaID).mkString(", ")
      end serials
      serials = serials.patch(serials.lastIndexOf(", "), " & ", 1)
      serials = if serials.startsWith("& ") then serials.replaceFirst("& ", "") else serials

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
