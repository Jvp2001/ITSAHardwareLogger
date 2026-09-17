package org.itsadigitaltrust.hardwarelogger.core

import com.mysql.cj.jdbc.exceptions.CommunicationsException
import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.hardwarelogger.dialogs.Dialogs
import org.scalafx.extras
import org.scalafx.extras.ShowMessage

import java.lang.Thread.UncaughtExceptionHandler
import java.util.concurrent.TimeoutException
import scala.util.control.NonFatal


class HardwareLoggerDefaultUncaughtExceptionHandler extends UncaughtExceptionHandler with HWLLoggable:
  override def uncaughtException(t: Thread, e: Throwable): Unit =
    extras.onFX:
      e match
        case _: AssertionError => ()
        case _:CommunicationsException =>
          Dialogs.showDBConnectionError()
        case NonFatal(e) =>
          ShowMessage.exception("Error", "Exception has occurred",  e.asInstanceOf[Throwable])

        case e: TimeoutException =>
          ShowMessage.exception("Timeout", "Timeout has occurred", e)
        case e: Throwable =>
          logger.error("Uncaught exception", e)
  
    