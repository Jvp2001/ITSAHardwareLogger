package org.itsadigitaltrust.hardwarelogger.viewmodels.tabs

import org.itsadigitaltrust.hardwarelogger.services.ApplicationServicesRegistry
import org.itsadigitaltrust.hardwarelogger.viewmodels.HardwareLoggerRootViewModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.tabs.*

trait ViewModelsModule:
  this: ApplicationServicesRegistry =>

  final given rootViewModel: HardwareLoggerRootViewModel = HardwareLoggerRootViewModel()
  final given generalInfoTabViewModel: GeneralInfoTabViewModel = GeneralInfoTabViewModel()
  final given memoryTabViewModel: MemoryTabViewModel = MemoryTabViewModel()
  final given hardDriveTabViewModel: HardDrivesTabViewModel = HardDrivesTabViewModel()
  final given processorTabViewModel: ProcessorTabViewModel = ProcessorTabViewModel()
  final given mediaTabViewModel: MediaTabViewModel = MediaTabViewModel()
  final given batteryStatusTabViewModel: BatteryStatusTabViewModel = BatteryStatusTabViewModel()
