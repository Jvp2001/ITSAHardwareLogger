package org.itsadigitaltrust.hardwarelogger.viewmodels.tabs

import org.itsadigitaltrust.common.Operators.{??, |>}
import org.itsadigitaltrust.common.types.DataSizeType.total
import org.itsadigitaltrust.hardwarelogger.services.HardwareGrabberService
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState
import org.itsadigitaltrust.hardwarelogger.viewmodels.ViewModel

import scala.Conversion.into
import scala.math.Numeric.Implicits.infixNumericOps


final class MemoryTabViewModel(using hardwareGrabberService: HardwareGrabberService, applicationState: HardwareLoggerApplicationState) extends ViewModel: //TableViewItemsDataContainer(MemoryTableRowViewModel.apply, _.memory): //TabTableViewModel[MemoryModel, MemoryTableRowViewModel](MemoryTableRowViewModel.apply, _.memory):

  def totalMemoryValue: String =
    applicationState.memory.map: mem =>
      val result = mem.map(_.size)
        .map: i =>
          val long: into[Long] = i
          long
        .reduce: (a,b) =>
          a + b
      result.toString + " GB"
    ?? "-42 GB"


end MemoryTabViewModel

 




