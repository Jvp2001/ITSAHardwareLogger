package org.itsadigitaltrust.hardwarelogger.tasks

import org.itsadigitaltrust.common.Operators.{toOption, |>}
import org.itsadigitaltrust.hardwarelogger.core.HWLoggerShowMessage
import org.scalafx.extras.BusyWorker.SimpleTask
import org.itsadigitaltrust.hardwarelogger.core.ui.*
import scalafx.stage.Window
import org.scalafx.extras
import ox.OxError

import java.util.concurrent.Future
import scala.Conversion.into
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import ox.either.{fail, ok}

import scala.math.Fractional.Implicits.infixFractionalOps
import scala.math.Integral.Implicits.infixIntegralOps
import scala.util.boundary


class HardwareLoggerBusyWorker(title: String, window: Option[Window] = None)(using private val statusMessageProp: into[Option[StringProperty]],
                                                                             private val progressProp: into[Option[DoubleProperty]], private val busyProp: into[Option[BooleanProperty]] = None) extends extras.BusyWorker(title, window):
  statusMessageProp.foreach:
    _ <== progressMessage

  progressProp.foreach:
    _ <== progressValue

  busyProp.foreach:
    _ <== busy

end HardwareLoggerBusyWorker

