package org.itsadigitaltrust.hardwarelogger.core

import org.itsadigitaltrust.hardwarelogger.views.Dialogs
import scalafx.application.Platform

import java.lang.Thread.UncaughtExceptionHandler
import java.sql.SQLException


class HardwareLoggerDefaultUncaughtExceptionHandler extends UncaughtExceptionHandler:
  override def uncaughtException(t: Thread, e: Throwable): Unit =
    Platform.runLater:
      Dialogs.showErrorAlert(e.getClass.getSimpleName, e.getMessage)
    e.printStackTrace(System.err)
  
    