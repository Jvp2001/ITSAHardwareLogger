package org.itsadigitaltrust.hardwarelogger.tasks
import org.itsadigitaltrust.hardwarelogger.tasks.HardwareLoggerTaskContext.{Foreground, WaitBackground, WaitForeground}
import org.itsadigitaltrust.hardwarelogger.tasks.ScalaFXHardwareLoggerTaskRunner

import scala.annotation.targetName

import scala.language.experimental.relaxedLambdaSyntax


//
//import org.itsadigitaltrust.common.Operators.{??, |>}
//
//import org.itsadigitaltrust.hardwarelogger.tasks
//import org.itsadigitaltrust.hardwarelogger.tasks.HardwareLoggerTaskContext.{Background, Foreground, WaitBackground, WaitForeground, waitBackground}
//
//import scala.Conversion.into
//import scala.annotation.targetName
//import scala.compiletime.deferred
//import scala.concurrent.duration.{Duration, DurationInt}
//
//trait HardwareLoggerGroupTaskGroupBuilder[R, FC <: HardwareLoggerTaskContext.Foreground, BC <: HardwareLoggerTaskContext.Background, +FGB <: HardwareLoggerTaskGroupBuilder[FC, R], +BGB <: HardwareLoggerTaskGroupBuilder[BC, R]]:
//
//  type Context[C <: HardwareLoggerTaskContext] = C match
//    case Background => BGB#C
//    case Foreground => FGB#C
//
//  type ForegroundTaskBuilder = FGB
//  type BackgroundTaskBuilder = BGB
//
//  protected type Func[C] = C ?=> R
//  @targetName("addForegroundTask")
//  def -->(op: => Func[FC])(using context: FC): this.type
//
//  @targetName("addBackgroundTask")
//  infix def <--(op: => Func[BC])(using background: BC): this.type
//
//  @targetName("addForegroundTasksOperator")
//  def ->>(ops: => Func[FC]*)(using foreground: FC): this.type
//
//  @targetName("addBackgroundTasks")
//  def <<-(ops: => Seq[Func[BC]])(using background: BC): this.type
//
//  @targetName("addForegroundTasksNonInfix")
//  def addAll(name: String)(ops: => Func[FC]*)(using foreground: FC): this.type
//
//  @targetName("addBackgroundTasksNonInfix")
//  def addAll(name: String, timeout: Duration)(ops: => Func[BC]*)(using background: BC): this.type
//
//  @targetName("addForegroundTaskNonInfix")
//  def add(name: String)(op: => Func[FC])(using foreground: FC): this.type
//
//  @targetName("addBackgroundTaskNonInfix")
//  def add(name: String, timeout: Duration)(op: => Func[BC])(using background: BC): this.type
//
//  @targetName("addForegroundTasks")
//  def addAll(ops: => Func[FC]*)(using foreground: FC): this.type
//
//  @targetName("addBackgroundTasks")
//  def addAll(name: String, timeout: Duration)(ops: => Func[BC]*)(using background: BC): this.type
//
//end HardwareLoggerGroupTaskGroupBuilder
//
//
//class SimpleWaitHardwareLoggerGroupTaskGroupBuilder[R]
//(using mapForegroundOp: (WaitForeground ?=> R) => WaitForeground ?=> HLTaskResult[R] =
//  (op: WaitForeground ?=> R) =>
//    val result = op
//    if op == null then
//      Left(HLTaskErrorCode.UnknownError)
//    else
//      Right(result)
//, mapBackgroundOp: (WaitBackground ?=> R) => WaitBackground ?=> HLTaskResult[R] =
//    (op: WaitBackground ?=> R) =>
//      val result = op
//      if op == null then
//        Left(HLTaskErrorCode.UnknownError)
//      else
//        Right(result)
//) extends HardwareLoggerGroupTaskGroupBuilder[R, WaitForeground, WaitBackground, SimpleHardwareLoggerWaitForegroundTaskGroupBuilder[R], SimpleHardwareLoggerBackgroundTaskGroupBuilder[R]]:
//
//
//  import org.itsadigitaltrust.hardwarelogger.tasks.HardwareLoggerTaskContext.*
//
//  type Context[C <: HardwareLoggerTaskContext] = C match
//    case Background => WaitBackground
//    case Foreground => WaitForeground
//
//  override type ForegroundTaskBuilder = SimpleHardwareLoggerWaitForegroundTaskGroupBuilder[R]
//  override type BackgroundTaskBuilder = SimpleHardwareLoggerBackgroundTaskGroupBuilder[R]
//
//
//  val foregroundTasks = new ForegroundTaskBuilder()
//  val backgroundTasks = new BackgroundTaskBuilder()
//
//  @targetName("addForegroundTask")
//  def -->(op: => Func[WaitForeground])(using context: WaitForeground): this.type =
//    foregroundTasks.add("Task", context.timeout)(op)
//    this
//
//  @targetName("addBackgroundTask")
//  def <--(op: => Func[WaitBackground])(using background: BackgroundTaskBuilder#C): this.type =
//    import background.given
//    backgroundTasks.add("Task", summon[Duration])(mapBackgroundOp(op))
//    this
//
//  @targetName("addForegroundTasksOperator")
//  def ->>(ops: => Func[WaitForeground]*)(using foreground: WaitForeground): this.type =
//    foregroundTasks.addAll(ops *)
//    this
//
//  @targetName("addBackgroundTasks")
//  override def <<-(ops: => Seq[Func[WaitBackground]])(using background: WaitBackground): SimpleWaitHardwareLoggerGroupTaskGroupBuilder.this.type =
//    backgroundTasks.addAll(ops*)
//    this
//
//
//  @targetName("addForegroundTasksNonInfix")
//  def addAll(name: String)(ops: => Func[WaitForeground]*)(using foreground: WaitForeground): this.type =
//    ops.foreach(op => foregroundTasks.add(name, foreground.timeout)(mapForegroundOp(op)))
//    this
//
//  @targetName("addBackgroundTasksNonInfix")
//  def addAll(name: String, timeout: Duration)(ops: => Func[WaitForeground]*)(using background: WaitForeground): this.type =
//    ops.foreach(op => backgroundTasks.add(name, timeout)(mapBackgroundOp(op)))
//    this
//
//
//  @targetName("addForegroundTaskNonInfix")
//  def add(name: String)(op: => Func[WaitForeground])(using foreground: WaitForeground): this.type =
//    foregroundTasks.add(name, 1.seconds)(mapForegroundOp(op))
//    this
//
//  @targetName("addBackgroundTaskNonInfix")
//  def add(name: String, timeout: Duration)(op: => Func[WaitBackground])(using background: WaitBackground): this.type =
//    backgroundTasks.add(name, timeout)(mapBackgroundOp(op))
//    this
//
//  @targetName("addForegroundTasks")
//  def addAll(ops: => Func[WaitForeground]*)(using foreground: WaitForeground): this.type =
//    foregroundTasks.addAll(ops *)
//    this
//
//  @targetName("addBackgroundTasks")
//  def addAll(ops: => Func[WaitBackground]*)(using background: WaitBackground): this.type =
//    ops.foreach(op => backgroundTasks.add("Task", background.timeout)(mapBackgroundOp(op)))
//    this
//
//  def run(name: String = "")(onSucceeded: () => Unit = () => ()): Unit =
//    backgroundTasks.run("Background Tasks"): () =>
//      foregroundTasks.run("Foreground Tasks")(onSucceeded)
//
//end SimpleWaitHardwareLoggerGroupTaskGroupBuilder
//
//object HardwareLoggerGroupTaskGroupBuilder:
//
//
//  given globalSimpleWaitHardwareLoggerGroupTaskGroupBuilder[R]: SimpleWaitHardwareLoggerGroupTaskGroupBuilder[R] with
//    summon[SimpleWaitHardwareLoggerGroupTaskGroupBuilder[R]]
//end HardwareLoggerGroupTaskGroupBuilder


class SimpleWaitHardwareLoggerGroupTaskGroupBuilder[R]:
  val foregroundTasks: ScalaFXHardwareLoggerTaskRunner[Foreground,R ] = ScalaFXHardwareLoggerTaskRunner()
  val backgroundTasks: ScalaFXHardwareLoggerTaskRunner[WaitBackground, R] = ScalaFXHardwareLoggerTaskRunner()

  def -->(op: => R)(using context: WaitForeground): this.type =
    foregroundTasks.add("Task")(op)
    this

  def <--(op: => R)(using background: WaitBackground): this.type =
    backgroundTasks.add("Task")(op)
    this

  def ->>(ops: Seq[WaitBackground ?=> R])(using background: WaitBackground): this.type =
    ops.foreach(op => backgroundTasks.add("Task")(op))
    this

  def <<-(ops: Seq[WaitBackground ?=> R])(using foreground: WaitForeground): this.type =
    val add = foregroundTasks.add("Task")
    ops.foreach(op => add(op))
    this

  def addAll(name: String)(ops: => Seq[WaitForeground ?=> R])(using foreground: WaitForeground): this.type =
    ops.foreach: op =>
      foregroundTasks.add(name)(op)
    this

  def addAll(name: String)(ops: WaitBackground ?=> R*)(using background: WaitBackground): this.type =
    ops.foreach: op =>
      backgroundTasks.add(name)(op)
    this

  @targetName("addAllForegroundTasksWithoutName")
  def addAll(ops: WaitForeground ?=> R*)(using foreground: WaitForeground): this.type =
    ops.foreach: op =>
      foregroundTasks.add("Task")(op)
    this

  @targetName("addAllBackgroundTasksWithoutName")
  def addAll(ops: WaitBackground ?=> R*)(using background: WaitBackground): this.type =
    ops.foreach: op =>
      backgroundTasks.add("Task")(op)
    this

  def add(name: String)(op: => WaitForeground ?=> R)(using foreground: WaitForeground): this.type =
    foregroundTasks.add(name)(op)
    this

  def add(name: String)(op: => WaitBackground ?=> R)(using background: WaitBackground): this.type =
    backgroundTasks.add(name)(op)
    this

  def add(op: => WaitForeground ?=> R)(using foreground: WaitForeground): this.type =
    foregroundTasks.add("Task")(op)
    this

  def add(op: => WaitBackground ?=> R)(using background: WaitBackground): this.type =
    backgroundTasks.add("Task")(op)
    this


  def run(name: String = "")(onSucceeded: => Unit = ()): Unit =
    backgroundTasks.run(name): () => 
      foregroundTasks.run(name): () =>
        onSucceeded  

end SimpleWaitHardwareLoggerGroupTaskGroupBuilder

object SimpleWaitHardwareLoggerGroupTaskGroupBuilder:
  given globalTaskBuilder[R]: SimpleWaitHardwareLoggerGroupTaskGroupBuilder[R] with
    new SimpleWaitHardwareLoggerGroupTaskGroupBuilder[R]
