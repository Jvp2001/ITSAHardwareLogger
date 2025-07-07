package org.itsadigitaltrust.hardwarelogger.core

import org.itsadigitaltrust.hardwarelogger.dialogs.Dialogs
import org.itsadigitaltrust.hardwarelogger.services.ServicesModule
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.NotificationName

import com.mysql.cj.exceptions.CJCommunicationsException
import com.mysql.cj.jdbc.exceptions.CommunicationsException
import scalafx.application.Platform

import java.lang.Thread.UncaughtExceptionHandler
import java.sql.SQLException
import java.util.ConcurrentModificationException


class HardwareLoggerDefaultUncaughtExceptionHandler extends UncaughtExceptionHandler with ServicesModule:
  override def uncaughtException(t: Thread, e: Throwable): Unit =
    Platform.runLater:
      e match
        case _:CommunicationsException =>
          notificationCentre.post(NotificationName.Reload)
          Dialogs.showDBConnectionError()
        case _: ConcurrentModificationException => ()
//          notificationCentre.post(NotificationName.Reload)
        case _: NumberFormatException =>
          ()
        case _: NullPointerException => ()
        case _ =>
          Dialogs.showErrorAlert(e.getClass.getSimpleName, e.getMessage)
    e.printStackTrace(System.err)
  
    