package org.itsadigitaltrust.hardwarelogger.delegates

import org.itsadigitaltrust.hardwarelogger.services.{NotificationCentre, NotificationChannel, ServicesModule}
import scalafx.beans.property.StringProperty

type ProgramMode = "Normal" | "HardDrive"
trait ProgramModeChangedDelegate extends ServicesModule:
  notificationCentre.subscribe(NotificationChannel.ProgramModeChanged): (key, args) =>
    if args.length == 1 then
      val firstArg = args(0)
      onProgramModeChanged(firstArg.asInstanceOf[ProgramMode])

  def onProgramModeChanged(mode: ProgramMode): Unit = ()


object ProgramMode extends ServicesModule:
  private val currentProgramMode = StringProperty("Normal")

  def mode_=(mode: ProgramMode): Unit =
    currentProgramMode.value = mode

  def mode: ProgramMode =
    currentProgramMode.value.asInstanceOf[ProgramMode]

  currentProgramMode.addListener: (_, _, newValue) =>
    notificationCentre.publish(NotificationChannel.ProgramModeChanged, newValue)