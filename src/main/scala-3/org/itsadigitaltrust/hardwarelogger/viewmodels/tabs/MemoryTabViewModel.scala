package org.itsadigitaltrust.hardwarelogger.viewmodels.tabs

import org.itsadigitaltrust.common.Operators.{??, |>}
import org.itsadigitaltrust.common.types.DataSizeType.total
import org.itsadigitaltrust.hardwarelogger.models.HLModel.MemoryModel
import org.itsadigitaltrust.hardwarelogger.services.HardwareGrabberService
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState
import org.itsadigitaltrust.hardwarelogger.viewmodels.ViewModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.MemoryTableRowViewModel

import scala.Conversion.into
import scala.math.Numeric.Implicits.infixNumericOps


final class MemoryTabViewModel(using hardwareGrabberService: HardwareGrabberService) extends TabTableViewModel(MemoryTableRowViewModel.apply, _.memory):

  def totalMemoryValue: String =
    try
      val memory = applicationSharedData.memory ?? Seq()
      if memory.isEmpty then
        logger.warn("No memory modules were reported; leaving the total memory blank")
        ""
      else
        memory.map(_.size).total.toGiBString
    catch
      case t: Throwable =>
        logger.error("Failed to compute the total memory", t)
        ""
  end totalMemoryValue



end MemoryTabViewModel






