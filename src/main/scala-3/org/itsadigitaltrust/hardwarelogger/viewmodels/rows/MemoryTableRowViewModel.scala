package org.itsadigitaltrust.hardwarelogger.viewmodels.rows

import org.itsadigitaltrust.hardwarelogger.models.MemoryModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.TableRowViewModel
import scalafx.beans.property.*
import org.itsadigitaltrust.common.types.*
import org.itsadigitaltrust.common.types.DataSizeType.DataSizeUnit.GB

import scala.math.Numeric.Implicits.infixNumericOps

final class MemoryTableRowViewModel(model: MemoryModel)(using itsaID: String) extends TableRowViewModel[MemoryModel](model):

  def sizeProperty: StringProperty =
    wrapper.field("size", _.size.dbString.replace("i", ""), "0 GB")(StringProperty.apply)

  def descriptionProperty: StringProperty =
    wrapper.field("description", _.description, "")(StringProperty.apply)

  def typeProperty: StringProperty =
    wrapper.field("type", _.`type`,"")(StringProperty.apply)