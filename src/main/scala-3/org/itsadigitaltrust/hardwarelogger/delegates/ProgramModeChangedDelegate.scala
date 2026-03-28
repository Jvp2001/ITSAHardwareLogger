package org.itsadigitaltrust.hardwarelogger.delegates

import org.itsadigitaltrust.common.Operators.or

import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{NotificationCentreService, NotificationName, ProgramModeChangedArgs}
import org.itsadigitaltrust.hardwarelogger.services.{ApplicationServicesRegistry, NotificationCentreModule}

import scalafx.beans.property.*
import scalafx.Includes.{*, given}
import scalafx.beans.property
import sun.jvm.hotspot.runtime.PerfMemory.end

type ProgramMode = "Normal" | "HardDrive" | "Both"

object ProgramMode extends ApplicationServicesRegistry:
  private val currentProgramMode = StringProperty("Normal")
  private var oldMode: ProgramMode = "Normal"

  val isModeNormal: BooleanProperty = new BooleanProperty()
  isModeNormal.value = currentProgramMode.value == "Normal"


  val isHardDriveMode: BooleanProperty = new BooleanProperty
  isHardDriveMode.value = currentProgramMode.value == "HardDrive"

  private def postProgramModeChangedNotification(oldMode: String, newMode: String): Unit =
    notificationCentre.post[ProgramModeChangedArgs](NotificationName.ProgramModeChanged, ProgramModeChangedArgs(oldMode.asInstanceOf[ProgramMode], newMode.asInstanceOf[ProgramMode]))
  currentProgramMode.onChange: (_, oldValue, newValue) =>
    isModeNormal.value = newValue == "Normal"
    isHardDriveMode.value  = newValue == "HardDrive"
    postProgramModeChangedNotification(oldValue, newValue)


  def isInNormalMode: Boolean = isModeNormal.get
  def isInHardDriveMode: Boolean = isHardDriveMode.get
  def mode_=(mode: ProgramMode): Unit =
    val oldMode = currentProgramMode.value.asInstanceOf[ProgramMode]
    currentProgramMode.value = mode
    notificationCentre.post(NotificationName.ProgramModeChanged, ProgramModeChangedArgs(oldMode, mode))

  def mode: ProgramMode =
    currentProgramMode.value.asInstanceOf[ProgramMode]

  currentProgramMode.addListener: (_, oldValue, newValue) =>
    postProgramModeChangedNotification(oldValue, newValue)