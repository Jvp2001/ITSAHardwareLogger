package org.itsadigitaltrust.hardwarelogger.viewmodels.tabs

import org.itsadigitaltrust.common.types.DataSizeType.DataSize
import org.itsadigitaltrust.hardwarelogger.models.MemoryModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.MemoryTableRowViewModel
import scalafx.Includes.*
import org.itsadigitaltrust.common.types.given
import scalafx.beans.property.StringProperty


final class MemoryTabViewModel extends TabTableViewModel[MemoryModel, MemoryTableRowViewModel](MemoryTableRowViewModel.apply, _.memory):
  val totalMemoryProperty: StringProperty = StringProperty("0 GiB")
  data.onChange: (source, change) =>
    totalMemoryProperty.value = source.map: datum =>
      datum.sizeProperty.get
    .sum(summon[Numeric[DataSize]])
    .toString + " MiB"




