package org.itsadigitaltrust.hardwarelogger.viewmodels.rows

import org.itsadigitaltrust.hardwarelogger.models.ProcessorModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.TableRowViewModel
import scalafx.beans.property.*

final class ProcessorTableRowViewModel(model: ProcessorModel)(using itsaID: String = "") extends TableRowViewModel[ProcessorModel](model):
  def nameProperty: StringProperty =
    println(s"Name Prop val: ${model.name}")
    wrapper.field("name", _.name, "")(StringProperty.apply)

  def speedProperty: LongProperty =
    wrapper.field[Long, LongProperty, LongProperty]("speed", _.speed, 0L)(LongProperty.apply)

  def shortDescriptionProperty: StringProperty =
    wrapper.field("shortDescription", _.shortDescription, "")(StringProperty.apply)

  def longDescriptionProperty: StringProperty =
    wrapper.field("longDescription", _.longDescription, "")(StringProperty.apply)

  def serialProperty: StringProperty =
    wrapper.field("serial", _.serial, "")(StringProperty.apply)

  def widthProperty: IntegerProperty =
    wrapper.field[Int, IntegerProperty, IntegerProperty]("width", _.width, 0)(IntegerProperty.apply)

  def coresProperty: IntegerProperty =
    wrapper.field[Int, IntegerProperty, IntegerProperty]("cores", _.cores, 0)(IntegerProperty.apply)
