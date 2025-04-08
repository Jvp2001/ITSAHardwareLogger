package org.itsadigitaltrust.hardwarelogger.viewmodels


import org.itsadigitaltrust.common
import org.itsadigitaltrust.common.Success
import org.itsadigitaltrust.hardwarelogger.HardwareLoggerApplication.{databaseService, getClass}
import org.itsadigitaltrust.hardwarelogger.services.HardwareIDValidationService.ValidationError
import org.itsadigitaltrust.hardwarelogger.services.NotificationChannel.{ContinueWithDuplicateDrive, DBSuccess, Reload, Save, ShowDuplicateDriveWarning}
import scalafx.beans.property.*
import org.itsadigitaltrust.hardwarelogger.services.{HardwareIDValidationService, NotificationCentre, NotificationChannel, ServicesModule}
import scalafx.scene.control.{Alert, ButtonType}
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.Alert.AlertType.{Information, Warning}


final class HardwareLoggerRootViewModel extends ViewModel with ServicesModule:



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

  notificationCentre.subscribe(NotificationChannel.Reload): (key, _) =>
    new Alert(Information):
      headerText = "Loaded"
    .showAndWait()

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
        notificationCentre.publish(Reload)

  override def setup(): Unit =
    reload()

  def save(): Unit =
    validateID(true)

    if idErrorStringProperty.isEmpty.get then
      databaseService.itsaid = idStringProperty.get()
      notificationCentre.publish(Save)

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
  end validateID




  def reload(): Unit =
    hardwareGrabberService.load()