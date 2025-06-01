package org.itsadigitaltrust.hardwarelogger.viewmodels.rows

import org.itsadigitaltrust.hardwarelogger.models.MemoryModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.TableRowViewModel
import scalafx.beans.property.*
import org.itsadigitaltrust.common.types.*

import scala.math.Numeric.Implicits.infixNumericOps

final class MemoryTableRowViewModel(model: MemoryModel)(using itsaID: String) extends TableRowViewModel[MemoryModel](model):
  def sizeProperty: DoubleProperty =
    wrapper.field[Double, DoubleProperty, DoubleProperty]("size", _.size.toLong, 0)(DoubleProperty.apply)


  def descriptionProperty: StringProperty =
    wrapper.field("description", _.description, "")(StringProperty.apply)