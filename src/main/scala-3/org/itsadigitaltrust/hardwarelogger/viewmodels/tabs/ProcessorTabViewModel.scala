package org.itsadigitaltrust.hardwarelogger.viewmodels.tabs

import org.itsadigitaltrust.hardwarelogger.contextbasedemnubar.MenuManagerService
import org.itsadigitaltrust.hardwarelogger.models.HLModel.ProcessorModel
import org.itsadigitaltrust.hardwarelogger.services.HardwareGrabberService
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.ProcessorTableRowViewModel

// To this (Explicitly name and type the service instance):
final class ProcessorTabViewModel(using HardwareGrabberService, MenuManagerService)
  extends TabTableViewModel(ProcessorTableRowViewModel.apply, _.processors)

  
  

