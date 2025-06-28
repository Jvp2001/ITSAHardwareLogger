package org.itsadigitaltrust.hardwarelogger.viewmodels.tabs

import org.itsadigitaltrust.common.Operators.??

import org.itsadigitaltrust.common.types.DataSizeType.{DataSize, DataSizeUnit}
import org.itsadigitaltrust.hardwarelogger.models.MemoryModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.MemoryTableRowViewModel
import scalafx.Includes.*
import scalafx.beans.property.StringProperty


final class MemoryTabViewModel(using itsaID: String) extends TabTableViewModel[MemoryModel, MemoryTableRowViewModel](MemoryTableRowViewModel.apply, _.memory):
  val totalMemoryProperty: StringProperty = StringProperty("")

  data.onChange: (source, change) =>
    totalMemoryProperty.value = source.map: datum =>
      datum.sizeProperty.value.split(" ").head.toDouble
    .sum
    .toString + " GB"






