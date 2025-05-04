package org.itsadigitaltrust.hardwarelogger.tasks

import org.scalafx.extras.batch.ItemTask

type TaskFunction[R]= () => R
sealed class HardwareLoggerTask[R](override val name: String)(function: TaskFunction[R]) extends ItemTask[R]:

  override def run(): R =
    function()

final class HardwareGrabberTask[R](function: TaskFunction[R]) extends HardwareLoggerTask[R]("HardwareGrabberTask")(function)

final class DatabaseTransactionTask(function: TaskFunction[Unit]) extends HardwareLoggerTask[Unit]("DatabaseTransactionTask")(function)


