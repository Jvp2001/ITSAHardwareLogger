package org.itsadigitaltrust.hardwarelogger.viewmodels.rows

import javafx.beans.property.*
import org.itsadigitaltrust.hardwarelogger.viewmodels.TableRowViewModel

//final class HardDriveTableRowViewModel(
//  private val healthProp: IntegerProperty,
//  private val sizeProp: StringProperty,
//  private val modelProp: StringProperty,
//  private val serialProp: StringProperty,
//  private val typeProp: StringProperty,
//  private val idProp: StringProperty,
//  private val isSSDProp: BooleanProperty
//) extends TableRowViewModel:
//  def healthProperty: IntegerProperty = healthProp
//  def health_=(newValue: Int): Unit = healthProp.setValue(newValue)
//  def health: Int = healthProp.get()
//
//  def sizeProperty: StringProperty = sizeProp
//  def size_=(newValue: String): Unit = sizeProp.setValue(newValue)
//  def size: String = sizeProp.get()
//
//  def modelProperty: StringProperty = modelProp
//  def model_=(newValue: String): Unit = modelProp.setValue(newValue)
//  def model: String = modelProp.get()
//
//  def serialProperty: StringProperty = serialProp
//  def serial_=(newValue: String): Unit = serialProp.setValue(newValue)
//  def serial: String = serialProp.get()
//
//  def typeProperty: StringProperty = typeProp
//  def type_=(newValue: String): Unit = typeProp.setValue(newValue)
//  def `type`: String = typeProp.get()
//
//  def idProperty: StringProperty = idProp
//  def id_=(newValue: String): Unit = idProp.setValue(newValue)
//  def id: String = idProp.get()
//
//  def isSSDProperty: BooleanProperty = isSSDProp
//  def isSSD_=(newValue: Boolean): Unit = isSSDProp.setValue(newValue)
//  def isSSD: Boolean = isSSDProp.get()