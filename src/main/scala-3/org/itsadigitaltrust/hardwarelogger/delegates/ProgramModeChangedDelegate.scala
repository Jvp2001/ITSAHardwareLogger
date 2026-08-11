package org.itsadigitaltrust.hardwarelogger.delegates

import org.itsadigitaltrust.hardwarelogger.services.ApplicationServicesRegistry
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{NotificationCentre, NotificationName}
import scalafx.beans.property.*

type ProgramMode = "Normal" | "HardDrive" | "Both"

object ProgramMode extends ApplicationServicesRegistry:
  private val currentProgramMode = StringProperty("Normal")
  private var oldMode: ProgramMode = "Normal"

  val isModeNormal: BooleanProperty = new BooleanProperty()
  isModeNormal.value = currentProgramMode.value == "Normal"


  val isHardDriveMode: BooleanProperty = new BooleanProperty
  isHardDriveMode.value = currentProgramMode.value == "HardDrive"

  def toggleMode(): Unit =
    if isModeNormal.value then
      currentProgramMode.value = "HardDrive"
    else
      currentProgramMode.value = "Normal"
  end toggleMode
  

  def addProgramModeChangedListener(f: ProgramMode => Unit): Unit =
    currentProgramMode.addListener: (_, _, newValue) =>
      f(newValue.asInstanceOf[ProgramMode])
  private def postProgramModeChangedNotification(oldMode: String, newMode: String): Unit =
    ox.supervised:
      NotificationCentre.programModeChanged(mode)
  currentProgramMode.onChange: (_, oldValue, newValue) =>
    isModeNormal.value = newValue == "Normal"
    isHardDriveMode.value  = newValue == "HardDrive"
    postProgramModeChangedNotification(oldValue, newValue)


  def isInNormalMode: Boolean = isModeNormal.get
  def isInHardDriveMode: Boolean = isHardDriveMode.get
  def mode_=(mode: ProgramMode): Unit =
    val oldMode = currentProgramMode.value.asInstanceOf[ProgramMode]
    currentProgramMode.value = mode
    postProgramModeChangedNotification(oldMode, mode)

  def mode: ProgramMode =
    currentProgramMode.value.asInstanceOf[ProgramMode]

  currentProgramMode.addListener: (_, oldValue, newValue) =>
    postProgramModeChangedNotification(oldValue, newValue)
    
  def apply(): Unit =
    NotificationCentre.programModeChanged(mode)