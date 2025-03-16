package org.itsadigitaltrust.hardwarelogger.viewmodels.rows

import org.itsadigitaltrust.hardwarelogger.models.{HardDrive, HardDriveType}
import org.itsadigitaltrust.hardwarelogger.viewmodels.TableRowViewModel
import scalafx.beans.property.*

import java.lang


final case class HardDriveTableRowViewModel(model: HardDrive) extends TableRowViewModel[HardDrive](model):

  def healthProperty: LongProperty =
    wrapper.field[Long, Number, LongProperty, LongProperty]("Health", _.health, 100L)(LongProperty.apply)

  def sizeProperty: LongProperty =
    wrapper.field[Long, Number, LongProperty, LongProperty]("Size", _.size, 0L)(LongProperty.apply)
    
  def modelProperty: StringProperty =
    wrapper.field("model", _.model, "")(StringProperty.apply)

  def serialProperty: StringProperty =
    wrapper.field("serial", _.serial, "")(StringProperty.apply)

  def typeProperty: ObjectProperty[HardDriveType] =
    wrapper.field("type", _.`type`, HardDriveType.SATA)(ObjectProperty.apply)

  def idProperty: StringProperty =
    wrapper.field("id", _.id, "NOT LOGGED")(StringProperty.apply)

  def isSSDProperty: BooleanProperty =
    wrapper.field[Boolean, lang.Boolean,BooleanProperty, BooleanProperty]("isSSD", _.isSSD, false)(BooleanProperty.apply)
    
    
  




//
//import javafx.beans.property.*
//import org.itsadigitaltrust.hardwarelogger.models.HardDrive
//import org.itsadigitaltrust.hardwarelogger.viewmodels.TableRowViewModel
//
////final class HardDriveTableRowViewModel(
////  private val healthProp: IntegerProperty,
////  private val sizeProp: StringProperty,
////  private val modelProp: StringProperty,
////  private val serialProp: StringProperty,
////  private val typeProp: StringProperty,
////  private val idProp: StringProperty,
////  private val isSSDProp: BooleanProperty
////) extends TableRowViewModel:
////  def healthProperty: IntegerProperty = healthProp
////  def health_=(newValue: Int): Unit = healthProp.setValue(newValue)
////  def health: Int = healthProp.get()
////
////  def sizeProperty: StringProperty = sizeProp
////  def size_=(newValue: String): Unit = sizeProp.setValue(newValue)
////  def size: String = sizeProp.get()
////
////  def modelProperty: StringProperty = modelProp
////  def model_=(newValue: String): Unit = modelProp.setValue(newValue)
////  def model: String = modelProp.get()
////
////  def serialProperty: StringProperty = serialProp
////  def serial_=(newValue: String): Unit = serialProp.setValue(newValue)
////  def serial: String = serialProp.get()
////
////  def typeProperty: StringProperty = typeProp
////  def type_=(newValue: String): Unit = typeProp.setValue(newValue)
////  def `type`: String = typeProp.get()
////
////  def idProperty: StringProperty = idProp
////  def id_=(newValue: String): Unit = idProp.setValue(newValue)
////  def id: String = idProp.get()
////
////  def isSSDProperty: BooleanProperty = isSSDProp
////  def isSSD_=(newValue: Boolean): Unit = isSSDProp.setValue(newValue)
////  def isSSD: Boolean = isSSDProp.get()
//
//
//
//final class HardDriveTableRowViewModel(model: HardDrive) extends TableRowViewModel[HardDrive](model):
//  def healthProperty: IntegerProperty =
//    wrapper.field("health", _.health, (m, v) => m.health = v, 0)(() => new SimpleIntegerProperty())
//
//  def sizeProperty: StringProperty =
//    wrapper.field("size", _.size, (m, v) => m.size = v, "")(() => new SimpleStringProperty())
//
//  def modelProperty: StringProperty =
//    wrapper.field("model", _.model, (m, v) => m.model = v, "")(() => new SimpleStringProperty())
//
//  def serialProperty: StringProperty =
//    wrapper.field("serial", _.serial, (m, v) => m.serial = v, "")(() => new SimpleStringProperty())
//
//  def typeProperty: ObjectProperty[HardDriveType] =
//    wrapper.field("type", _.`type`, (m, v) => m.`type` = v, HardDriveType.SATA)(() => new SimpleObjectProperty())
//
//  def idProperty: StringProperty =
//    wrapper.field("id", _.id, (m, v) => m.id = v, "NOT LOGGED")(() => new SimpleStringProperty())
//
//  def isSSDProperty: BooleanProperty =
//    wrapper.field("isSSD", _.isSSD, (m, v) => m.isSSD = v, false)(() => new SimpleBooleanProperty())
