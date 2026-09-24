package org.itsadigitaltrust.hardwarelogger.viewmodels.tabs

import org.itsadigitaltrust.hardwarelogger.core.HWLoggerShowMessage
import org.itsadigitaltrust.hardwarelogger.models.HLModel.BatteryStatusModel
import org.itsadigitaltrust.hardwarelogger.services.HardwareGrabberService
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.BatteryStatusTableRowViewModel
import scalafx.concurrent.{ScheduledService, Task} 

class BatteryStatusTabViewModel(using applicationState: HardwareLoggerApplicationState)(using HardwareGrabberService) extends TabTableViewModel(BatteryStatusTableRowViewModel.apply, _.battery) with HWLoggerShowMessage:
  val service =
    ScheduledService:
       Task[Unit]:
         ox.supervised:
          ox.fork:
            applicationState.reloadBattery()
          .join()
          Right(())
  end service
  
  service.onSucceeded = _ =>
    reload()
  

  service.period = javafx.util.Duration.seconds(2)
  service.restart()

end BatteryStatusTabViewModel

