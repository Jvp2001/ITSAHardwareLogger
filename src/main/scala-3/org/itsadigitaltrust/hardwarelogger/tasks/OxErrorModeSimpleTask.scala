package org.itsadigitaltrust.hardwarelogger.tasks

import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.hardwarelogger.core.HWLoggerShowMessage
import org.scalafx.extras.BusyWorker.SimpleTask
import scalafx.application.Platform
import scalafx.stage.Window

import java.util.concurrent.Future
import scala.concurrent.duration.{DurationInt, FiniteDuration}

trait OxErrorModeSimpleTask[E <: Throwable, R](using timeout: FiniteDuration = 5.seconds) extends SimpleTask[Either[E, R]], HWLoggerShowMessage, HWLLoggable:

  val exitIfFailed: Boolean = false

  override final def onFinish(result: Future[Either[E, R]], successful: Boolean): Unit =
    // Do not use the successful parameter, as I am not throwing exceptions from call.
    // Runs background, so .get
    try
      val eitherResult = result.get(timeout.toMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
      eitherResult match
        case Left(error) => throw error
        case Right(_) => onFinished(eitherResult)
    catch
      case scala.util.control.NonFatal(e) =>
        if exitIfFailed then
          Platform.runLater:
            showException("Exception", "Exception has occurred!", e)
            Platform.exit()
        else
          showException("Exception", "Exception has occurred!", e)
    finally
      message.unbind()
      progress.unbind()
    end try
  end onFinish

  def onFinished(either: Either[E, R]): Unit = ()

  override def call(): Either[E, R] =
    ox.supervisedError(ox.EitherMode[E]):
      val r = onCall()
      r

  @FunctionalInterface
  def onCall()(using context: ox.OxError[E, [X] =>> Either[E, X]]): Either[E, R]

  override protected def parentWindow: Option[Window] = None
end OxErrorModeSimpleTask
