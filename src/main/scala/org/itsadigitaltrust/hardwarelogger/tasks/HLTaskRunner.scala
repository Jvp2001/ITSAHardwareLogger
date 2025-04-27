package org.itsadigitaltrust.hardwarelogger.tasks

import org.itsadigitaltrust.hardwarelogger.services.ServicesModule
import org.scalafx.extras.BusyWorker
import org.scalafx.extras.BusyWorker.SimpleTask
import org.scalafx.extras.batch.{BatchRunnerWithProgress, ItemTask}
import scalafx.application.Platform

object HLTaskRunner extends ServicesModule:
  def run[T[_] <: ItemTask[?], U](title: String, args: (() => U)*)(ctor: (() => U) => T[U])(finished: () => Unit = () => ()): Unit =
    val busyWorker = new BusyWorker(title, Seq())
    busyWorker.doTask("Start"): () =>
      val batchRunnerWithProgress = new BatchRunnerWithProgress[U](title, None, true):
        override def createTasks(): Seq[ItemTask[U]] =
          args.map(ctor).map(_.asInstanceOf[ItemTask[U]])
      batchRunnerWithProgress.run()
    busyWorker.busy.onChange: (_, _, newValue) =>
      if !newValue then
        finished()


  def apply[T[_] <: ItemTask[?], U](title: String, args: (() => U)*)(ctor: (() => U) => T[U])(finished: () => Unit = () => ()): Unit =
    run(title, args *)(ctor)(finished)