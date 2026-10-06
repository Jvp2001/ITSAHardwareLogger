package org.itsadigitaltrust.hardwarelogger.delegates

import org.itsadigitaltrust.hardwarelogger.services.ApplicationServicesRegistry
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{NotificationCentre, ProgramModeChangedEvent}
import scalafx.beans.property.*

sealed trait ProgramOperationMode
trait HardDriveMode extends ProgramOperationMode
trait NormalMode extends ProgramOperationMode


type ProgramMode = "Normal" | "HardDrive" | "Both"

given Conversion[String, ProgramMode] with
  override def apply(x: String): ProgramMode = 
    x.asInstanceOf[ProgramMode]

object ProgramMode:
  private val currentProgramMode = StringProperty("Normal")
  private var oldMode: ProgramMode = "Normal"
  
  def modeProperty: StringProperty = currentProgramMode

  val isModeNormal: BooleanProperty = new BooleanProperty()
  isModeNormal <== modeProperty.isEqualTo("Normal")

  val isHardDriveMode: BooleanProperty = new BooleanProperty
  isHardDriveMode <== currentProgramMode.isEqualTo("HardDrive")

  def toggleMode(): Unit =
    if isModeNormal.value then
      currentProgramMode.value = "HardDrive"
    else
      currentProgramMode.value = "Normal"
  end toggleMode



  currentProgramMode.onChange: (_, oldValue, newValue) =>
    NotificationCentre.publish(ProgramModeChangedEvent(newValue))

  def isInNormalMode: Boolean = isModeNormal.get
  def isInHardDriveMode: Boolean = isHardDriveMode.get
  def mode_=(mode: ProgramMode): Unit =
    currentProgramMode() = mode

  def mode: ProgramMode =
    currentProgramMode.value.asInstanceOf[ProgramMode]

end ProgramMode
