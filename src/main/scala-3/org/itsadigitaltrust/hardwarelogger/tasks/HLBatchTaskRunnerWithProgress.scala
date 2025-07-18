package org.itsadigitaltrust.hardwarelogger.tasks

import org.scalafx.extras.batch.BatchRunnerWithProgress
import scalafx.stage.Window
//
abstract class HLBatchTaskRunnerWithProgress[U](
                                                 override val title: String,
                                                 override val parentWindow: Option[Window],
                                                 override val useParallelProcessing: Boolean
                                               ) extends BatchRunnerWithProgress[U](title, parentWindow, useParallelProcessing):

  override def run(): Seq[BatchRunnerWithProgress.TaskResult[U]] =
    try
      super.run()
    catch
      case _: Exception =>
        Seq.empty