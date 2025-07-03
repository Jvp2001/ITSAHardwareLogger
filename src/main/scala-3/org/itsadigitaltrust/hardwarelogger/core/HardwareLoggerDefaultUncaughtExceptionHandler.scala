package org.itsadigitaltrust.hardwarelogger.core

import org.itsadigitaltrust.hardwarelogger.dialogs.Dialogs
import com.mysql.cj.exceptions.CJCommunicationsException
import com.mysql.cj.jdbc.exceptions.CommunicationsException
import scalafx.application.Platform

import java.lang.Thread.UncaughtExceptionHandler
import java.sql.SQLException


class HardwareLoggerDefaultUncaughtExceptionHandler extends UncaughtExceptionHandler:
  override def uncaughtException(t: Thread, e: Throwable): Unit =
    Platform.runLater:
      e match
        case _:CommunicationsException =>
          Dialogs.showDBConnectionError()
        case _ =>
          Dialogs.showErrorAlert(e.getClass.getSimpleName, e.getMessage)
    e.printStackTrace(System.err)
  
    