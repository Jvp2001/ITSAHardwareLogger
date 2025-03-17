//package org.itsadigitaltrust.hardwarelogger.viewmodels.rows
//
//import javafx.beans.property.*
//import org.itsadigitaltrust.hardwarelogger.models.{GeneralInfoModel, Processor}
//import org.itsadigitaltrust.hardwarelogger.mvvm.properties.accessorfunctions.{StringGetter, StringSetter}
//import org.itsadigitaltrust.hardwarelogger.viewmodels.TableRowViewModel
////
////final class ProcessorTableViewModel(
////  private val chipTypeProp: StringProperty,
////  private val speedProp: StringProperty,
////  private val shortDescriptionProp: StringProperty,
////  private val longDescriptionProp: StringProperty,
////  private val serialProp: StringProperty,
////  private val widthProp: IntegerProperty,
////  private val coresProp: IntegerProperty
////) extends TableRowViewModel:
////  def chipTypeProperty: StringProperty = chipTypeProp
////  def chipType_=(newValue: String): Unit = chipTypeProp.setValue(newValue)
////  def chipType: String = chipTypeProp.get()
////
////  def speedProperty: StringProperty = speedProp
////  def speed_=(newValue: String): Unit = speedProp.setValue(newValue)
////  def speed: String = speedProp.get()
////
////  def shortDescriptionProperty: StringProperty = shortDescriptionProp
////  def shortDescription_=(newValue: String): Unit = shortDescriptionProp.setValue(newValue)
////  def shortDescription: String = shortDescriptionProp.get()
////
////  def longDescriptionProperty: StringProperty = longDescriptionProp
////  def longDescription_=(newValue: String): Unit = longDescriptionProp.setValue(newValue)
////  def longDescription: String = longDescriptionProp.get()
////
////  def serialProperty: StringProperty = serialProp
////  def serial_=(newValue: String): Unit = serialProp.setValue(newValue)
////  def serial: String = serialProp.get()
////
////  def widthProperty: IntegerProperty = widthProp
////  def width_=(newValue: Int): Unit = widthProp.setValue(newValue)
////  def width: Int = widthProp.get()
////
////  def coresProperty: IntegerProperty = coresProp
////  def cores_=(newValue: Int): Unit = coresProp.setValue(newValue)
////  def cores: Int = coresProp.get()
//
//
//final class GeneralInfoTableRowViewModel(model: GeneralInfoModel) extends TableRowViewModel[GeneralInfoModel](model):
//  def computerIDProperty: StringProperty =
//    wrapper.field("computerID", _.computerID, (m, v) => m.copy(computerID = v), "")(() => new SimpleStringProperty())
//
//  def descriptionProperty: StringProperty =
//    wrapper.field("description", _.description, (m, v) => m.copy(description = v), "")(() => new SimpleStringProperty())
//
//  def modelProperty: StringProperty =
//    wrapper.field("model", _.model, (m, v) => m.copy(model = v), "")(() => new SimpleStringProperty())
//
//  def vendorProperty: StringProperty =
//    wrapper.field("vendor", _.vendor, (m, v) => m.copy(vendor = v), "")(() => new SimpleStringProperty())
//
//  def serialProperty: StringProperty =
//    wrapper.field("serial", _.serial, (m, v) => m.copy(serial = v), "")(() => new SimpleStringProperty())
//
//  def osProperty: StringProperty =
//    wrapper.field[String, Property[String], StringGetter, StringSetter]("os", _.os, (m, v) => m.copy(os = v), "")(() => new SimpleStringProperty())
//
//final class ProcessorTableRowViewModel(model: Processor) extends TableRowViewModel[Processor](model):
//  def chipTypeProperty: StringProperty =
//    wrapper.field("chipType", _.chipType, (m, v) => m.chipType = v, "")(() => new SimpleStringProperty())
//
//  def speedProperty: StringProperty =
//    wrapper.field("speed", _.speed, (m, v) => m.speed = v, "")(() => new SimpleStringProperty())
//
//  def shortDescriptionProperty: StringProperty =
//    wrapper.field("shortDescription", _.shortDescription, (m, v) => m.shortDescription = v, "")(() => new SimpleStringProperty())
//
//  def longDescriptionProperty: StringProperty =
//    wrapper.field("longDescription", _.longDescription, (m, v) => m.longDescription = v, "")(() => new SimpleStringProperty())
//
//  def serialProperty: StringProperty =
//    wrapper.field("serial", _.serial, (m, v) => m.serial = v, "")(() => new SimpleStringProperty())
//
//  def widthProperty: IntegerProperty =
//    wrapper.field("width", _.width, (m, v) => m.width = v, 0)(() => new SimpleIntegerProperty())
//
//  def coresProperty: IntegerProperty =
//    wrapper.field("cores", _.cores, (m, v) => m.cores = v, 0)(() => new SimpleIntegerProperty())