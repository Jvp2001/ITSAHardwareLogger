package org.itsadigitaltrust.hardwarelogger.viewmodels.rows

import org.itsadigitaltrust.hardwarelogger.models.HLModel.BatteryStatusModel
import org.itsadigitaltrust.hardwarelogger.services.HardwareGrabberService
import scalafx.beans.property.{DoubleProperty, StringProperty}
import org.itsadigitaltrust.hardwarelogger.core.DataTypes.*
final class BatteryStatusTableRowViewModel(model: BatteryStatusModel)(using HardwareGrabberService) extends TableRowViewModel[BatteryStatusModel](model):
  def statusProperty: StringProperty =
    wrapper.field("status", _.status, "")(StringProperty.apply)
    
  def chargeProperty: DoubleProperty =
    wrapper.field[Double, DoubleProperty, DoubleProperty]("charge", _.charge, 0.0D)(DoubleProperty.apply)

  def healthProperty: DoubleProperty =
    wrapper.field[Double, DoubleProperty, DoubleProperty]("health", _.health.toDouble, 0.0D)(DoubleProperty.apply)
    
  
  
