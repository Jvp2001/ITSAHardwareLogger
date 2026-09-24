package org.itsadigitaltrust.hardwarelogger.core

import com.mysql.cj.jdbc.exceptions.CommunicationsException
import org.itsadigitaltrust.common.logging.HWLLoggable

import org.scalafx.extras
import org.scalafx.extras.ShowMessage

import java.lang.Thread.UncaughtExceptionHandler
import java.util.concurrent.TimeoutException
import scala.util.control.NonFatal


class HardwareLoggerDefaultUncaughtExceptionHandler extends UncaughtExceptionHandler with HWLoggerShowMessage:
  override def uncaughtException(t: Thread, e: Throwable): Unit =
    extras.onFX:
      e match
        case _: AssertionError => ()
        case e:CommunicationsException =>
          showException("DB Connection Error", "Failed to connect to the database!", e)

        case NonFatal(e) =>
          showException("Error", "Exception has occurred", e)

        case e: TimeoutException =>
          showException("Timeout", "Timeout has occurred", e)
        case e: Throwable =>
          logger.error("Uncaught exception", e)
  
    