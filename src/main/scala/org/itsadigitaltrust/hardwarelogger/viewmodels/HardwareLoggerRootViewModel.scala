package org.itsadigitaltrust.hardwarelogger.viewmodels

import org.itsadigitaltrust.hardwarelogger.services.HardwareIDValidationService.ValidationError
import scalafx.beans.property.*
import org.itsadigitaltrust.hardwarelogger.services.{HardwareIDValidationService, NotificationCentre, ServicesModule}
import scalafx.scene.control.{Alert, ButtonType}
import scalafx.scene.control.Alert.AlertType


final class HardwareLoggerRootViewModel extends ViewModel with ServicesModule:



  val idFieldFocusProperty: BooleanProperty = BooleanProperty(false)
  val validIDProperty: BooleanProperty = BooleanProperty(false)
  val idStringProperty: StringProperty = StringProperty("")
  val idErrorStringProperty: StringProperty = StringProperty("")

  private val idErrorAlert = new Alert(AlertType.Error, "", ButtonType.OK):
    contentText <== idErrorStringProperty

  validIDProperty.bind(idErrorStringProperty.isNotEmpty)

  idStringProperty.onChange: (observable, oldValue, newValue) =>
    if newValue.isEmpty || newValue == null then
      idErrorStringProperty.value = "ID must not be empty!"


  def save(): Unit =
    println("Validating...")


    validateID(true)

    if validIDProperty.get() then
      println("Saving...")


  def validateID(showAlert: Boolean = false): Unit =

    val currentID: String = Option(idStringProperty.get()).getOrElse("")
    if currentID == "" then
      idErrorStringProperty.set("ID cannot be empty!")
      return

    val result = hardwareIDValidationService.validate(currentID.strip())
    result match
      case Left(value) =>
        idErrorStringProperty.setValue(value.toString)
        if showAlert then
          value match
            case ValidationError.ParserError(error) => idErrorStringProperty.value = error.toString
            case error@ValidationError.IncorrectCheckDigit(expected, got) =>

              idErrorAlert.showAndWait() match
                case _ => idFieldFocusProperty.value = true
      case Right(value) =>
        idErrorStringProperty.setValue("")

  def reload(): Unit =
    notificationCentre.publish("RELOAD")
