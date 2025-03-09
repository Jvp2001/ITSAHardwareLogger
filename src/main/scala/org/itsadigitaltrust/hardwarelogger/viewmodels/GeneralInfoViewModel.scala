package org.itsadigitaltrust.hardwarelogger.viewmodels

import javafx.beans.property.*

final class GeneralInfoViewModel(
                                  private val computerIDProp: StringProperty,
                                  private val descriptionProp: StringProperty,
                                  private val modelProp: StringProperty,
                                  private val vendorProp: StringProperty,
                                  private val serialProp: StringProperty,
                                  private val osProp: StringProperty
                                ) extends ViewModel:

  def computerIDProperty: StringProperty = computerIDProp
  def computerID_=(newValue: String): Unit = computerIDProp.setValue(newValue)
  def computerID: String = computerIDProp.get()

  def descriptionProperty: StringProperty = descriptionProp
  def description_=(newValue: String): Unit = descriptionProp.setValue(newValue)
  def description: String = descriptionProp.get()

  def modelProperty: StringProperty = modelProp
  def model_=(newValue: String): Unit = modelProp.setValue(newValue)
  def model: String = modelProp.get()

  def vendorProperty: StringProperty = vendorProp
  def vendor_=(newValue: String): Unit = vendorProp.setValue(newValue)
  def vendor: String = vendorProp.get()

  def serialProperty: StringProperty = serialProp
  def serial_=(newValue: String): Unit = serialProp.setValue(newValue)
  def serial: String = serialProp.get()

  def osProperty: StringProperty = osProp
  def os_=(newValue: String): Unit = osProp.setValue(newValue)
  def os: String = osProp.get()
