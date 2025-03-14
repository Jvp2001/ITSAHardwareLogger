//package org.itsadigitaltrust.hardwarelogger.viewmodels.rows
//
//import javafx.beans.property.*
//import org.itsadigitaltrust.hardwarelogger.models.Media
//import org.itsadigitaltrust.hardwarelogger.viewmodels.TableRowViewModel
//
//
////final class MediaTableRowViewModel(
////  private val descriptionProp: StringProperty,
////  private val handleProp: StringProperty
////) extends TableRowViewModel:
////  def descriptionProperty: StringProperty = descriptionProp
////  def description_=(newValue: String): Unit = descriptionProp.setValue(newValue)
////  def description: String = descriptionProp.get()
////
////  def handleProperty: StringProperty = handleProp
////  def handle_=(newValue: String): Unit = handleProp.setValue(newValue)
////  def handle: String = handleProp.get()
//
//
//
//final class MediaTableRowViewModel(model: Media) extends TableRowViewModel[Media](model):
//  def descriptionProperty: StringProperty =
//    wrapper.field("description", _.description, (m, v) => m.description = v, "")(() => new SimpleStringProperty())
//
//  def handleProperty: StringProperty =
//    wrapper.field("handle", _.handle, (m, v) => m.handle = v, "")(() => new SimpleStringProperty())
