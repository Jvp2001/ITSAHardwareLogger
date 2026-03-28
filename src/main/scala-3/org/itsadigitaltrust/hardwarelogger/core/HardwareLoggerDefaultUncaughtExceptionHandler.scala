package org.itsadigitaltrust.hardwarelogger.core

import org.itsadigitaltrust.common.logging.HWLLoggable

import org.itsadigitaltrust.hardwarelogger.dialogs.Dialogs
import org.itsadigitaltrust.hardwarelogger.services.ApplicationServicesRegistry
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{NotificationName, ReloadMessageArgs}

import com.mysql.cj.exceptions.CJCommunicationsException
import com.mysql.cj.jdbc.exceptions.CommunicationsException
import scalafx.application.Platform

import java.lang.Thread.UncaughtExceptionHandler
import java.sql.SQLException
import java.util.ConcurrentModificationException


class HardwareLoggerDefaultUncaughtExceptionHandler extends UncaughtExceptionHandler with ApplicationServicesRegistry with HWLLoggable:
  override def uncaughtException(t: Thread, e: Throwable): Unit =
    Platform.runLater:
      e match
        case _: AssertionError => ()
        case _:CommunicationsException =>
          notificationCentre.post[ReloadMessageArgs](NotificationName.Reload, ReloadMessageArgs())
          Dialogs.showDBConnectionError()
        case _: ConcurrentModificationException => ()
//          notificationCentre.post(NotificationName.Reload)
        case _: NumberFormatException =>
          ()
        case _: NullPointerException => ()
        case _ =>
//          Dialogs.showErrorAlert(e.getClass.getSimpleName, e.getMessage)
    logger.whenTraceEnabled:
      ()
  
    