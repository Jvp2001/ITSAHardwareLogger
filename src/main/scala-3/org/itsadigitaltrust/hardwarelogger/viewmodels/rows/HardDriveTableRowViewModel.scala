package org.itsadigitaltrust.hardwarelogger.viewmodels.rows

import org.itsadigitaltrust.common.Operators.|>
import org.itsadigitaltrust.common.types.DataSizeType.*
import org.itsadigitaltrust.hardwarelogger.models.{HardDriveConnectionType, HardDriveModel}
import org.itsadigitaltrust.hardwarelogger.services.HardwareGrabberService
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.ProgramModeChangeNotificationListener
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState
import org.scalafx.extras.generic_pane.GenericDialogFX
import scalafx.beans.property.*
import scalafx.scene.control.TextArea


final case class HardDriveTableRowViewModel(_model: HardDriveModel)(using HardwareGrabberService, HardwareLoggerApplicationState) extends TableRowViewModel[HardDriveModel](_model):


  def healthProperty: StringProperty =
    wrapper.field("Health", _.health.toString, "100%")(StringProperty.apply)

  def performanceProperty: StringProperty =
    wrapper.field("Performance", _.performance.toString, "100%")(StringProperty.apply)
    
  def sizeProperty: StringProperty =
    wrapper.field("Size", _.size.toGBString, "0 GB")(StringProperty.apply)
    
  def modelProperty: StringProperty =
    wrapper.field("model", _.model, "")(StringProperty.apply)

  def serialProperty: StringProperty =
    wrapper.field("serial", _.serial, "")(StringProperty.apply)

  def typeProperty: ObjectProperty[HardDriveConnectionType] =
    wrapper.field("type", _.connectionType, HardDriveConnectionType.SATA)(ObjectProperty.apply)

  def idProperty: StringProperty =
    wrapper.field("id", _.itsaID, "NOT LOGGED")(StringProperty.apply)

  def driveTypeProperty: StringProperty =
    wrapper.field("isSSD", _.`type`, "SSD")(StringProperty.apply)
    
    
  

