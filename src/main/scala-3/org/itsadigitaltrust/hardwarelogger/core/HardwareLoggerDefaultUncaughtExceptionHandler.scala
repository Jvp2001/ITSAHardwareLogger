package org.itsadigitaltrust.hardwarelogger.core

import com.mysql.cj.jdbc.exceptions.CommunicationsException
import org.itsadigitaltrust.common.logging.HWLLoggable
import org.scalafx.extras
import org.scalafx.extras.ShowMessage
import scalafx.application.Platform
import scalafx.stage.{Stage, Window}

import java.lang.Thread.{UncaughtExceptionHandler, currentThread}
import java.util.concurrent.TimeoutException
import scala.util.control.NonFatal


class HardwareLoggerDefaultUncaughtExceptionHandler extends UncaughtExceptionHandler with HWLoggerShowMessage:
  var currentStage: Option[Stage] = None

  override protected def parentWindow: Option[Window] = currentStage
  override def uncaughtException(t: Thread, e: Throwable): Unit =
    Platform.runLater:
      e match
        case _: AssertionError => ()
        case e:CommunicationsException =>
          showException("DB Connection Error", "Failed to connect to the database!", e)

        case NonFatal(e) =>
          logger.error("Non-fatal exception", e)

        case e: TimeoutException =>
          showException("Timeout", "Timeout has occurred", e)
        case e: Throwable =>
          logger.error("Uncaught exception", e)
  
    