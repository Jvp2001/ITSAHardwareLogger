package org.itsadigitaltrust.hardwarelogger.viewmodels

import org.itsadigitaltrust.hardwarelogger.core.NotificationCentre

final class HardwareLoggerRootViewModel(private val notificationCentre: NotificationCentre) extends ViewModel:
  
  def reload(): Unit =
    notificationCentre.publish("RELOAD")
