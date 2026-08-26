package org.itsadigitaltrust.hardwarelogger.core

import com.mysql.cj.jdbc.exceptions.CommunicationsException
import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.hardwarelogger.dialogs.Dialogs
import org.itsadigitaltrust.hardwarelogger.services.ApplicationServicesRegistry
import org.itsadigitaltrust.hardwarelogger.services.OshiHardwareGrabberApplicationService.HardwareGrabberException
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{NotificationCentre, NotificationName}
import org.scalafx.extras
import org.scalafx.extras.ShowMessage
import scalafx.application.Platform

import java.lang.Thread.UncaughtExceptionHandler
import java.util.ConcurrentModificationException
import scala.concurrent.ExecutionException
import scala.util.boundary.Break


class HardwareLoggerDefaultUncaughtExceptionHandler extends UncaughtExceptionHandler with HWLLoggable:
  override def uncaughtException(t: Thread, e: Throwable): Unit =
    extras.onFX:
      e match
        case _: AssertionError => ()
        case _:CommunicationsException =>
          ()
          Dialogs.showDBConnectionError()
        case _: ConcurrentModificationException => ()
//         NotificationCentre.post(NotificationName.Reload)
        case _: NumberFormatException =>
          ()
        case _: NullPointerException => ()
        case b: Break[?] =>
          ShowMessage.exception("Hardware Grabber Exception", "", b.value.asInstanceOf[Throwable])

        case e: ExecutionException =>
          ShowMessage.exception("Execution Exception", "", e.getCause)
        case exception: Exception =>
          ShowMessage.exception("Error", "Exception has occurred", exception)

    logger.whenTraceEnabled:
      ()


  
    