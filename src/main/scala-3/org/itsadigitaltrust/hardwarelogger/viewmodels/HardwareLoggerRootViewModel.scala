package org.itsadigitaltrust.hardwarelogger.viewmodels


import org.itsadigitaltrust.common
import org.itsadigitaltrust.common.Operators.??
import org.itsadigitaltrust.common.Success
import org.itsadigitaltrust.hardwarelogger.HardwareLoggerApplication.{databaseService, getClass}
import org.itsadigitaltrust.hardwarelogger.delegates.{ProgramMode, ProgramModeChangedDelegate}
import org.itsadigitaltrust.hardwarelogger.services.HardwareIDValidationService.ValidationError
import org.itsadigitaltrust.hardwarelogger.services.NotificationChannel.{ContinueWithDuplicateDrive, DBSuccess, Reload, Save, ShowDuplicateDriveWarning}
import scalafx.beans.property.*
import org.itsadigitaltrust.hardwarelogger.services.{HardwareIDValidationService, NotificationCentre, NotificationChannel, ServicesModule}
import scalafx.application.Platform
import scalafx.scene.control.{Alert, ButtonType}
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.Alert.AlertType.{Information, Warning}


final class HardwareLoggerRootViewModel extends ViewModel with ServicesModule with ProgramModeChangedDelegate:

  val idFieldFocusProperty: BooleanProperty = BooleanProperty(false)
  val validIDProperty: BooleanProperty = BooleanProperty(false)
  val idStringProperty: StringProperty = StringProperty("")
  val idErrorStringProperty: StringProperty = StringProperty("")

  private val idErrorAlert = new Alert(AlertType.Error, "", ButtonType.OK):
    contentText <== idErrorStringProperty

  validIDProperty <== idErrorStringProperty.isNotEmpty

  idStringProperty.onChange: (observable, oldValue, newValue) =>
    if newValue.isEmpty || newValue == null then
      idErrorStringProperty.value = "ID must not be empty!"
    else
      validateID()

  notificationCentre.subscribe(DBSuccess): (key, _) =>
    new Alert(Information, "Data has been saved!", ButtonType.OK).showAndWait()

  notificationCentre.subscribe(ShowDuplicateDriveWarning): (key, args: Seq[Any]) =>
    val  serial = args.head.asInstanceOf[String]
    new Alert(Warning, "Duplicate Drive Found!", ButtonType.Yes, ButtonType.No):
      contentText = s"A drive with the serial number '$serial' already exists. Do you want to continue?"
    .showAndWait() match
      case Some(ButtonType.Yes) =>
        notificationCentre.publish(ContinueWithDuplicateDrive)
      case _ =>
        reload()

  notificationCentre.subscribe(NotificationChannel.Reload): (key, _) =>
    val pcInfo = Option(hardwareGrabberService.generalInfo)
    if pcInfo.isEmpty then
      ()
    val info = pcInfo.get
    val itsaId = info.itsaID match
      case Some(value) => value
      case None => ""
    idStringProperty.value = itsaId
     println(s"Itsa ID: $itsaId")

  override def setup(): Unit =
    reload()

  def save(): Unit =
    validateID(true)
    if idErrorStringProperty.isEmpty.get then
      databaseService.itsaId = idStringProperty.get()
      notificationCentre.publish(Save)
  end save


  def validateID(showAlert: Boolean = false): Unit =
    val currentID: String = Option(idStringProperty.get()).getOrElse("")
    if currentID == "" then
      idErrorStringProperty.set("ID cannot be empty!")
      return

    val result = hardwareIDValidationService.validate(currentID.strip())
    result match
      case  org.itsadigitaltrust.common.Error(value) =>
        idErrorStringProperty.value = value.toString
        if showAlert then
          value match
            case ValidationError.ParserError(error) => idErrorStringProperty.value = error.toString
            case error@ValidationError.IncorrectCheckDigit(expected, got) =>

              idErrorAlert.showAndWait() match
                case _ => idFieldFocusProperty.value = true
      case Success(value) =>
        idErrorStringProperty.value = ""
        idStringProperty.value = value.toString
  end validateID

  def reload(): Unit =
    hardwareGrabberService.load(): () =>
      notificationCentre.publish(NotificationChannel.Reload)
      idStringProperty.value = hardwareGrabberService.generalInfo.itsaID ?? ""
      validIDProperty.value = hardwareIDValidationService.validate(idStringProperty.value).toBoolean
      val notWipedDrives = hardwareGrabberService.hardDrives.filterNot: hardDrive =>
        databaseService.findWipingRecord(hardDrive.serial) match
          case Some(value) => true
          case None => false

      var serials = notWipedDrives.map(_.serial).mkString(", ")
      serials = serials.patch(serials.lastIndexOf(", "), "& ", 1)
      val word = serials.length match
        case 0 => ""
        case 1 => "drive"
        case _ => "drives"

      if word == "" then
        ()

      new Alert(Information, s"The $word '$serials' have not been logged. Do you want to log them now?", ButtonType.Yes, ButtonType.No):
        headerText = "No Wiping Records Found"

      .showAndWait() match
        case Some(value) if value == ButtonType.Yes =>
        case None => ???
  end reload
  
  override def onProgramModeChanged(mode: ProgramMode): Unit =
    reload()
  hardwareIDValidationService.validate(idStringProperty.get)

end HardwareLoggerRootViewModel
