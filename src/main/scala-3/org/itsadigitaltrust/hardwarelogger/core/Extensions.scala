package org.itsadigitaltrust.hardwarelogger.core

// On purpose to mimic extension method
import org.itsadigitaltrust.common.Operators.??
import org.itsadigitaltrust.common.orThrowOption
import org.itsadigitaltrust.hardwarelogger.core.{HWLoggerShowMessage, ShowMessageHWLogger}
import org.itsadigitaltrust.hardwarelogger.models.HLModel.MemoryModel
import org.itsadigitaltrust.hardwarelogger.tasks.OxErrorModeSimpleTask
import org.itsadigitaltrust.common.total
import org.scalafx.extras
import org.scalafx.extras.BusyWorker.SimpleTask
import org.scalafx.extras.{ShowMessage, ShowMessageLogger}
import ox.*
import ox.either.{fail, ok}
import scalafx.application.Platform

import java.util.concurrent.Future
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.boundary
import scala.math.Numeric.Implicits.infixNumericOps

extension(memory: Option[Seq[MemoryModel]])
  inline def total: Double = 
    memory.map(_.map(_.size))
    .map(_.total.toDouble).getOrElse(0.0)

extension [E <: Throwable, R](a: Either[E, R])
  def showAndLogIfFailed()(using showMessage: HWLoggerShowMessage): Unit =
    a.left.map: value =>
      showMessage.showException("Exception", "Execution has occurred!", value)

  def showAndExitIfFailed()(using showMessage: HWLoggerShowMessage): Unit =
    showAndLogIfFailed()
    Platform.exit()

end extension

