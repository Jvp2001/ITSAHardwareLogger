package org.itsadigitaltrust.hardwarelogger.delegates

import org.itsadigitaltrust.hardwarelogger.services.{NotificationCentre, NotificationChannel, ServicesModule}
import scalafx.beans.property.*
import scalafx.Includes.{*, given}
import scalafx.beans.property

type ProgramMode = "Normal" | "HardDrive"
trait ProgramModeChangedDelegate extends ServicesModule:
  notificationCentre.subscribe(NotificationChannel.ProgramModeChanged): (key, args) =>
    if args.length == 1 then
      val firstArg = args(0)
      onProgramModeChanged(firstArg.asInstanceOf[ProgramMode])

  def onProgramModeChanged(mode: ProgramMode): Unit = ()


object ProgramMode extends ServicesModule:
  private val currentProgramMode = StringProperty("Normal")


  val isModeNormal: BooleanProperty = new BooleanProperty()
  isModeNormal.value = currentProgramMode.value == "Normal"


  val isHardDriveMode: BooleanProperty = new BooleanProperty
  isHardDriveMode.value = currentProgramMode.value == "HardDrive"

  currentProgramMode.onChange: (_, _, newValue) =>
    isModeNormal.value = newValue == "Normal"
    isHardDriveMode.value  = newValue == "HardDrive"

  def isInNormalMode: Boolean = isModeNormal.get
  def isInHardDriveMode: Boolean = isHardDriveMode.get
  def mode_=(mode: ProgramMode): Unit =
    currentProgramMode.value = mode

  def mode: ProgramMode =
    currentProgramMode.value.asInstanceOf[ProgramMode]

  currentProgramMode.addListener: (_, _, newValue) =>
    notificationCentre.publish(NotificationChannel.ProgramModeChanged, newValue)