package org.itsadigitaltrust.hardwarelogger.viewmodels.rows

import org.itsadigitaltrust.common.Types.*
import org.itsadigitaltrust.hardwarelogger.models.{HardDriveConnectionType, HardDriveModel}
import org.itsadigitaltrust.hardwarelogger.viewmodels.TableRowViewModel
import scalafx.beans.property.*
import org.itsadigitaltrust.common.percent
import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode

import java.lang


final case class HardDriveTableRowViewModel(model: HardDriveModel) extends TableRowViewModel[HardDriveModel](model):

  override protected val modeToSaveIn: ProgramMode | "both" = "both"
  
  def healthProperty: ObjectProperty[Percentage] =
    wrapper.field("Health", _.health, 100.percent)(ObjectProperty.apply)

  def performanceProperty: ObjectProperty[Percentage] =
    wrapper.field("Performance", _.health, 100.percent)(ObjectProperty.apply)
    
  def sizeProperty: StringProperty =
    wrapper.field("Size", _.size.dbString, "0 GB")(StringProperty.apply)
    
  def modelProperty: StringProperty =
    wrapper.field("model", _.model, "")(StringProperty.apply)

  def serialProperty: StringProperty =
    wrapper.field("serial", _.serial, "")(StringProperty.apply)

  def typeProperty: ObjectProperty[HardDriveConnectionType] =
    wrapper.field("type", _.connectionType, HardDriveConnectionType.SATA)(ObjectProperty.apply)

  def idProperty: StringProperty =
    wrapper.field("id", _.itsaID, "NOT LOGGED")(StringProperty.apply)

  def driveTypeProperty: StringProperty =
    wrapper.field("isSSD", _.`type`, "SSD")(StringProperty.apply)
    
    
  




//
//import javafx.beans.property.*
//import org.itsadigitaltrust.hardwarelogger.models.HardDriveModel
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
//final class HardDriveTableRowViewModel(model: HardDriveModel) extends TableRowViewModel[HardDriveModel](model):
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
