package org.itsadigitaltrust.hardwarelogger.viewmodels

import javafx.beans.property.{BooleanProperty, SimpleBooleanProperty, SimpleStringProperty, StringProperty}
import org.itsadigitaltrust.hardwarelogger.core.NotificationCentre
import org.itsadigitaltrust.hardwarelogger.services.HardwareIDValidationService
import org.springframework.stereotype.Component

@Component
final class HardwareLoggerRootViewModel(private val hardwareIDValidationService: HardwareIDValidationService, private val notificationCentre: NotificationCentre) extends ViewModel:

  val validIDProperty: BooleanProperty = SimpleBooleanProperty(false)
  val idStringProperty: StringProperty = SimpleStringProperty()
  val idErrorStringProperty: StringProperty = SimpleStringProperty()

  validIDProperty.bind(idErrorStringProperty.isNotEmpty)

  def save(): Unit =
    println("Validating...")


    validateID()

    if validIDProperty.get() then
      println("Saving...")
      
  
  def validateID(): Unit =

    val currentID: String = Option(idStringProperty.get()).getOrElse("")
    if currentID == "" then
      idErrorStringProperty.set("ID cannot be empty!")
      return

    val result = hardwareIDValidationService.validate(currentID.strip())
    result match
      case Left(value) =>
        idErrorStringProperty.setValue(value.toString)
      case Right(value) =>
        idErrorStringProperty.setValue("")

  def reload(): Unit =
    notificationCentre.publish("RELOAD")
