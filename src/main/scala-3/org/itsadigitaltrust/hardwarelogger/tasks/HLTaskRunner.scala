package org.itsadigitaltrust.hardwarelogger.tasks

import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{NotificationCentre, NotificationName}
import org.itsadigitaltrust.hardwarelogger.services.{HardwareGrabberService, ServicesModule}
import org.scalafx.extras.BusyWorker
import org.scalafx.extras.BusyWorker.SimpleTask
import org.scalafx.extras.batch.{BatchRunnerWithProgress, ItemTask}
import scalafx.application.Platform

import scala.collection.mutable

trait TaskExecutor[T[_]]:
  def executeTasks()(using notificationCentre: NotificationCentre[NotificationName])(using hardwareGrabberService: HardwareGrabberService): Unit

object HLTaskRunner extends ServicesModule:
  def run[T[_] <: ItemTask[?], U](title: String, taskFuncs: TaskFunction[U]*)(ctor: => TaskFunction[U] => T[U])(finished: () => Unit = () => ()): Unit =
    try
      val busyWorker = new BusyWorker(title, Seq())
      busyWorker.doTask("Start"): () =>
        val batchRunnerWithProgress = new BatchRunnerWithProgress[U](title, None, true):
          override def createTasks(): Seq[ItemTask[U]] =
            taskFuncs.map(ctor).map(_.asInstanceOf[ItemTask[U]])
        batchRunnerWithProgress.run()
      busyWorker.busy.onChange: (_, _, newValue) =>
        if !newValue then
          finished()
    catch
      case e: NumberFormatException =>
        Platform.runLater: () =>
          System.out.println(s"Error in HLTaskRunner: ${e.getMessage}")
          e.printStackTrace()
          finished()


  def apply[T[_] <: ItemTask[?], U](title: String, args: (() => U)*)(ctor: TaskFunction[U] => T[U])(finished: () => Unit = () => ()): Unit =
    run(title, args *)(ctor)(finished)

  // Function that takes a function that will run as a task
  def run(name: String)(taskFunc: TaskFunction[Unit]): Unit =
    run(name, taskFunc)(f => new HardwareLoggerTask[Unit](name)(f))(() => ())

  def runLater(name: String)(taskFunc: TaskFunction[Unit]): Unit =
    Platform.runLater: () =>
      run(name, taskFunc)
end HLTaskRunner

final class HLTaskGroupBuilder[T[_] <: ItemTask[?], U](ctor: => TaskFunction[U] => T[U]):
  private val tasks = mutable.ArrayBuffer[TaskFunction[U]]()

  def addAll(funcs: => U*): HLTaskGroupBuilder[T, U] =
    tasks ++= funcs.map: func =>
      () => func
    this

  def add(f: => U): HLTaskGroupBuilder[T, U] =
    tasks += (() => f)
    this

  def run(title: String)(finished: () => Unit = () => ()): Unit =
    HLTaskRunner(title, tasks.toSeq *)(ctor)(finished)

end HLTaskGroupBuilder