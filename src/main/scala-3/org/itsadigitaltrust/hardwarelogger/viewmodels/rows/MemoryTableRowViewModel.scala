package org.itsadigitaltrust.hardwarelogger.viewmodels.rows

import org.itsadigitaltrust.hardwarelogger.models.MemoryModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.TableRowViewModel
import scalafx.beans.property.*
import org.itsadigitaltrust.common.types.*
import org.itsadigitaltrust.hardwarelogger.core.DataSizeProperty
final class MemoryTableRowViewModel(model: MemoryModel) extends TableRowViewModel[MemoryModel](model):
  def sizeProperty: DataSizeProperty = 
    wrapper.field("size", _.size, DataSize(0, "GB"))(DataSizeProperty.apply)


  def descriptionProperty: StringProperty =
    wrapper.field("description", _.description, "")(StringProperty.apply)