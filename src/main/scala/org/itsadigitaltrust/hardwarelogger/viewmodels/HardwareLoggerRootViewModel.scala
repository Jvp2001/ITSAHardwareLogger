package org.itsadigitaltrust.hardwarelogger.viewmodels

import scalafx.beans.property.*
import org.itsadigitaltrust.hardwarelogger.services.{HardwareIDValidationService, NotificationCentre, ServicesModule}


final class HardwareLoggerRootViewModel extends ViewModel with ServicesModule:


  val validIDProperty: BooleanProperty = BooleanProperty(false)
  val idStringProperty: StringProperty = StringProperty("")
  val idErrorStringProperty: StringProperty = StringProperty("")

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
