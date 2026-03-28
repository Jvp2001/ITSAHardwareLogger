package org.itsadigitaltrust.hardwarelogger.viewmodels.rows

import org.itsadigitaltrust.hardwarelogger.models.MemoryModel
import scalafx.beans.property.*
import org.itsadigitaltrust.common.types.*
import org.itsadigitaltrust.common.types.DataSizeType.DataSizeUnit.GB

import scala.math.Numeric.Implicits.infixNumericOps
import org.itsadigitaltrust.hardwarelogger.services.given

final class MemoryTableRowViewModel(model: MemoryModel) extends TableRowViewModel[MemoryModel](model):

  def sizeProperty: StringProperty =
    wrapper.field("size", s => s.size.dbString, "0 GB")(StringProperty.apply)

  def descriptionProperty: StringProperty =
    wrapper.field("description", _.description, "")(StringProperty.apply)

  def typeProperty: StringProperty =
    wrapper.field("type", _.`type`,"")(StringProperty.apply)
    
  