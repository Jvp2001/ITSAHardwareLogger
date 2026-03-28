package org.itsadigitaltrust.hardwarelogger.tasks

//package org.itsadigitaltrust.hardwarelogger.tasks
//
//import org.itsadigitaltrust.common.logging.HWLLoggable
//
//import org.itsadigitaltrust.hardwarelogger.tasks.HardwareLoggerTaskContext.Givens.{WaitBackground, WaitForeground}
//import org.itsadigitaltrust.hardwarelogger.tasks.ScalaFXHardwareLoggerTaskRunner
//
import org.scalafx.extras.{BusyWorker, offFX, offFXAndWait, onFX, onFXAndWait}
import org.scalafx.extras.batch.{BatchRunnerWithProgress, ItemTask}
//import scalafx.stage.{Stage, Window}
//import sun.jvm.hotspot.runtime.PerfMemory.end
//
import org.itsadigitaltrust.common.logging.HWLLoggable

import org.itsadigitaltrust.hardwarelogger.tasks.{HardwareLoggerTask, HardwareLoggerTaskContext}

import scalafx.stage.Window

import scala.collection.mutable
//
trait HardwareLoggerTaskRunner[C <: HardwareLoggerTaskContext, R](using context: C) extends HWLLoggable:
  val tasks: mutable.ListBuffer[HardwareLoggerTask[C, R]] = mutable.ListBuffer()
//
//
  def += (task: HardwareLoggerTask[C, R]): Unit = tasks.addOne(task)
  def add(name: String)(op: => C ?=> R): this.type

  def apply(name: String = "Tasks", clearTasksOnSucceeded: Boolean = true)(onSucceeded: () => Unit = () => ()): Unit = run(name, clearTasksOnSucceeded)(onSucceeded)

  def run(name: String = "Tasks", clearTasksOnSucceeded: Boolean = true)(onSucceeded: () => Unit = () => ()): Unit

end HardwareLoggerTaskRunner

final class ScalaFXHardwareLoggerTaskRunner[C <: HardwareLoggerTaskContext, R](using window: Option[Window] = None)(using C) extends HardwareLoggerTaskRunner[C, R]:
  private class HardwareLoggerItemTask(task: HardwareLoggerTask[C, R]) extends ItemTask[R]:
      override def name: String = task.name

      override def run(): R =
        summon[C] match
          case c: HardwareLoggerTaskContext.foreground.type => onFX[R](task.op).asInstanceOf[R]
          case c: HardwareLoggerTaskContext.background.type => offFX[R](task.op).asInstanceOf[R]
          case c: HardwareLoggerTaskContext.waitBackground.type => offFXAndWait(task.op)
          case c: HardwareLoggerTaskContext.waitForeground.type => onFXAndWait(task.op)
  end HardwareLoggerItemTask
  private object HardwareLoggerItemTask:
    def apply(name: String)(op: => C ?=> R): HardwareLoggerItemTask =
      new HardwareLoggerItemTask(new SimpleHardwareLoggerTask[C, R](name, op))
    end apply
  def run(name: String = "Tasks", clearTasksOnSucceeded: Boolean = true)(onSucceeded: () => Unit = () => ()): Unit =
    val taskRunner = this

    new BusyWorker(name, window).doTask: () =>
      val runner: BatchRunnerWithProgress[R] = new BatchRunnerWithProgress[R](name, window, true):
        override def createTasks(): Seq[ItemTask[R]] = taskRunner.tasks.map(t => new HardwareLoggerItemTask(t)).toSeq
      val success =
        try
          runner.run()
          true
        catch
          case _ => false
      end success

      if success then onSucceeded()
  end run


  override def add(name: String)(op: => C ?=> R): this.type =
    tasks.addOne(new SimpleHardwareLoggerTask[C,R](name, op))
    this



end ScalaFXHardwareLoggerTaskRunner


type SimpleHardwareLoggerTaskGroupBuilder[C <: HardwareLoggerTaskContext.WaitHardwareLoggerTaskContext, R] = ScalaFXHardwareLoggerTaskRunner[C, R]
type SimpleWaitHardwareLoggerTaskGroupBuilder[C <: HardwareLoggerTaskContext.WaitHardwareLoggerTaskContext, R] = ScalaFXHardwareLoggerTaskRunner[C, R]
object ScalaFXHardwareLoggerTaskRunner:
  def runTasks[C <: HardwareLoggerTaskContext, R](name: String, tasks: Seq[HardwareLoggerTask[C, R]], window: Option[Window])(onSucceeded: => Unit = ())(using C): Unit =
    val runner = new ScalaFXHardwareLoggerTaskRunner[C,R]
    runner.apply(name)(() => onSucceeded)
end ScalaFXHardwareLoggerTaskRunner

