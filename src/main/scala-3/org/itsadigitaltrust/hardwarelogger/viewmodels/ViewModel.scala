package org.itsadigitaltrust.hardwarelogger.viewmodels

import org.itsadigitaltrust.common.logging.HWLLoggable

import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.models.HLModel
import org.itsadigitaltrust.hardwarelogger.mvvm.ModelWrapper
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.NotificationName.{Reload, Save}
import org.itsadigitaltrust.hardwarelogger.services.{ApplicationServicesRegistry, NotificationCentreModule}
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{NotificationCentreService, NotificationName}



trait ViewModel extends HWLLoggable with NotificationCentreModule:
  def setup(): Unit =
    reload()
    notificationCentre.addObserver(Reload)(reload)

    


  def reload(shouldClearData: Boolean = true): Unit = ()




    



  
  
