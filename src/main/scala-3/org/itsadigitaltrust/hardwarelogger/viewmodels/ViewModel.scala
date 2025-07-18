package org.itsadigitaltrust.hardwarelogger.viewmodels

import org.itsadigitaltrust.common.logging.HWLLoggable

import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.models.HLModel
import org.itsadigitaltrust.hardwarelogger.mvvm.ModelWrapper
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.NotificationName.Save
import org.itsadigitaltrust.hardwarelogger.services.ServicesModule
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{Notifiable, NotificationCentre, NotificationName}



trait ViewModel extends HWLLoggable:
  def setup(): Unit =
    reload()

  def reload(shouldClearData: Boolean = true): Unit = ()




    



  
  
