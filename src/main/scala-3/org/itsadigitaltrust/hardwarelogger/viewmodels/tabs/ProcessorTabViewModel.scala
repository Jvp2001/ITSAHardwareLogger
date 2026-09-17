package org.itsadigitaltrust.hardwarelogger.viewmodels.tabs

import org.itsadigitaltrust.hardwarelogger.models.HLModel.ProcessorModel
import org.itsadigitaltrust.hardwarelogger.services.HardwareGrabberService
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.ProcessorTableRowViewModel

final class ProcessorTabViewModel(using HardwareGrabberService) extends TabTableViewModel(ProcessorTableRowViewModel.apply, _.processors):
  override type RowViewModel = ProcessorTableRowViewModel
  override type Model = ProcessorModel
  
  

