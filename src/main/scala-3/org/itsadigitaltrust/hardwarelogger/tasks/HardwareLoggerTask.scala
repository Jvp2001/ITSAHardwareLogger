package org.itsadigitaltrust.hardwarelogger.tasks
//
//import org.itsadigitaltrust.common.Result
//
//import org.checkerframework.checker.units.qual.C
//
import scala.concurrent.TimeoutException
import scala.concurrent.duration.{Duration, DurationInt, SECONDS}


//enum HLTaskErrorCode:
//  case TaskTimeout extends HLTaskErrorCode
//  case TaskCancelled extends HLTaskErrorCode
//  case TaskInterrupted extends HLTaskErrorCode
//  case MaxRetriesExceeded extends HLTaskErrorCode
//  case InvalidContext extends HLTaskErrorCode
//  case ExecutionError[E](error: E) extends HLTaskErrorCode
//  case UnknownError extends HLTaskErrorCode
//  case ExceptionThrown(e: Exception) extends HLTaskErrorCode
//end HLTaskErrorCode
//
//type HLTaskResult[R] = R
//
//sealed class HardwareLoggerTask[C <: HardwareLoggerTaskContext, R](val name: String, op: => C ?=> Either[HLTaskErrorCode, R])(using context: C):
//  protected inline def callFunc(): HLTaskResult[R] =
//    try
//      val result: Either[HLTaskErrorCode, R] = op
//      result match
//        case left@Left(err) => Left(err)
//        case right@Right(res) => Right(res)
//    catch
//      case e: InterruptedException => Left(HLTaskErrorCode.TaskInterrupted)
//      case e: TimeoutException => Left(HLTaskErrorCode.TaskTimeout)
//      case e: HLTaskErrorCode => Left(e)
//      case e: Throwable => Left(HLTaskErrorCode.ExecutionError(e))
//      case e: Exception => Left(HLTaskErrorCode.ExceptionThrown(e))
//
//  def apply(): HLTaskResult[R] = callFunc()
//
//
//end HardwareLoggerTask
//
//object HardwareLoggerTask:
//
//  import HardwareLoggerTaskContext.{Foreground, Background}
//
//  final class ForegroundTask[C <: Foreground, R](override val name: String, op: => C ?=> HLTaskResult[R])(using context: C) extends HardwareLoggerTask[C, R](name, op)(using context)
//
//  final class BackgroundTask[C <: Background, R](override val name: String, op: => C ?=> HLTaskResult[R])(using context: C) extends HardwareLoggerTask[C, R](name, op)(using context)
//
//  // TaskFactory used by builders: a function that takes a name and returns a context-function that accepts an op and produces a Task
//  type TaskFactory[C <: HardwareLoggerTaskContext, R, Task[_ <: HardwareLoggerTaskContext, _ <: R]] = String => (=> C ?=> HLTaskResult[R]) ?=> Task[C, R]
//
//  // op is deferred so will only be evaluated if the task is run
//  def foregroundTask[C <: Foreground, R](name: String = "Task")(op: => C ?=> HLTaskResult[R])
//                       (using context: C): ForegroundTask[C, R] =
//    ForegroundTask[C, R](name, op)
//
//  // op is deferred so will only be evaluated if the task is run
//  def backgroundTask[C <: Background, R](name: String = "Task")(op: => C ?=> HLTaskResult[R])(using context: C): BackgroundTask[C, R] =
//    BackgroundTask[C, R](name, op)
//end HardwareLoggerTask
//
//
//object WaitHardwareLoggerTask:
//
//  import HardwareLoggerTaskContext.*
//
//  final class WaitForegroundTask[C <: WaitForeground, R](override val name: String, timeout: Duration, op: => C ?=> HLTaskResult[R])(using context: C) extends HardwareLoggerTask[C, R](name, op)
//  final class WaitBackgroundTask[C <: WaitBackground, R](override val name: String, timeout: Duration, op: => C ?=> HLTaskResult[R])(using context: C) extends HardwareLoggerTask[C, R](name, op)
//
//  def foregroundTask[C <: WaitForeground, R](name: String = "Task", timeout: Duration = 0.seconds)(op: => C ?=> HLTaskResult[R])
//  (using context: C): WaitForegroundTask[C, R] =
//    WaitForegroundTask[C, R](name, timeout, op)
//
//  def backgroundTask[C <: WaitBackground, R](name: String = "Task", timeout: Duration = 0.seconds)(op: => C ?=> HLTaskResult[R])
//  (using context: C): WaitBackgroundTask[C, R] =
//    WaitBackgroundTask[C, R](name, timeout, op)
//end WaitHardwareLoggerTask
//
//  private var retries: Long = 0
//
//  private def retry(): HLTaskResult[R] =
//    retries += 1
//    if isMaxRetriesExceeded then
//      Left(HLTaskErrorCode.MaxRetriesExceeded)
//    else
//      apply()
//
//  private def resetRetries(): Unit = retries = 0
//
//  private def isMaxRetriesExceeded: Boolean = retries >= maxRetries
//
//  override transparent inline def apply(): HLTaskResult[R] =
//    val result = callFunc()
//    if result.isLeft then
//      retry()
//    else
//      result
//
//// Context classes to make sure that service functions run on the correct thread
sealed trait HardwareLoggerTaskContext
//
object HardwareLoggerTaskContext:
  sealed trait Foreground extends HardwareLoggerTaskContext
//
  sealed trait Background extends HardwareLoggerTaskContext
  sealed trait WaitHardwareLoggerTaskContext extends HardwareLoggerTaskContext
  sealed trait WaitForeground extends WaitHardwareLoggerTaskContext, Foreground
  sealed trait WaitBackground extends WaitHardwareLoggerTaskContext, Background
  given foreground: Foreground with
  {}
  given background: Background with
  {}
//
  given waitForeground: WaitForeground with {}
//
  given waitBackground: WaitBackground with {}
end HardwareLoggerTaskContext

sealed trait HardwareLoggerTask[C <: HardwareLoggerTaskContext, R](val name: String, val op: C ?=> R)(using context: C)
class SimpleHardwareLoggerTask[C <: HardwareLoggerTaskContext, R](name: String, op: C ?=> R)(using context: C) extends HardwareLoggerTask[C, R](name, op)
sealed trait WaitHardwareLoggerTask[C <: HardwareLoggerTaskContext, R](name: String, timeout: Duration, op: C ?=> R)(using context: C) extends HardwareLoggerTask[C, R]

sealed trait WaitRetriableHardwareLoggerTask[C <: HardwareLoggerTaskContext, R](name: String, timeout: Duration, maxRetries: Long, op: C ?=> R )(using context: C) extends WaitHardwareLoggerTask[C, R]

object HardwareLoggerTask:
  class BackgroundTask[R](name: String, op: HardwareLoggerTaskContext.Background ?=> R)(using context: HardwareLoggerTaskContext.Background) extends HardwareLoggerTask[HardwareLoggerTaskContext.Background, R](name, op)
  class ForegroundTask[R](name: String, op: HardwareLoggerTaskContext.Foreground ?=> R)(using context: HardwareLoggerTaskContext.Foreground) extends HardwareLoggerTask[HardwareLoggerTaskContext.Foreground, R](name, op)
end HardwareLoggerTask
