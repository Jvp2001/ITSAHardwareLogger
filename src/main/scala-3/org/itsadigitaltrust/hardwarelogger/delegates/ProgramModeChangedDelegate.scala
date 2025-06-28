package org.itsadigitaltrust.hardwarelogger.delegates

import org.itsadigitaltrust.common.Operators.??


import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{Notifiable, NotificationCentre, NotificationName}
import org.itsadigitaltrust.hardwarelogger.services.ServicesModule

import scalafx.beans.property.*
import scalafx.Includes.{*, given}
import scalafx.beans.property

type ProgramMode = "Normal" | "HardDrive"
trait ProgramModeChangedDelegate extends ServicesModule with Notifiable[NotificationName]:
  override type Message = notificationCentre.type#Message
  override def onReceivedNotification(message: Message): Unit =
    if message.name == NotificationName.ProgramModeChanged then
      onProgramModeChanged(ProgramMode.mode)

  notificationCentre.addObserver(this)


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
    notificationCentre.post(NotificationName.ProgramModeChanged)