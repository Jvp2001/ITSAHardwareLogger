package org.itsadigitaltrust.hardwarelogger.tasks

import org.itsadigitaltrust.common.Operators.|>
import org.itsadigitaltrust.common.forkJoin
import org.itsadigitaltrust.hardwarelogger.core.HWLoggerShowMessage
import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.services.{HLDatabaseService, HardwareGrabberService}
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{NotificationCentre, NotificationCentreListener}
import org.itsadigitaltrust.hardwarelogger.shared.HardwareLoggerSharedData
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState
import org.scalafx.extras
import org.scalafx.extras.BusyWorker
import org.scalafx.extras.BusyWorker.SimpleTask
import ox.{EitherMode, Ox, OxError}
import ox.either.{ok, orThrow}
import scalafx.beans.property.{BooleanProperty, DoubleProperty, StringProperty}
import scalafx.stage.Window
import scalafx.util.Duration

import scala.Conversion.into
import scala.concurrent.duration.{DurationInt, FiniteDuration}

abstract class HardwareLoggerSimpleTask[E <: Throwable, R](var totalNumberOfItems: Int, val timeout: FiniteDuration = 3.seconds)(using showMessage: HWLoggerShowMessage) extends OxErrorModeSimpleTask[E, R], FiniteSimpleTask[Either[E, R]](totalNumberOfItems)

