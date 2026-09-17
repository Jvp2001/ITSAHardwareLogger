package org.itsadigitaltrust.hardwarelogger.viewmodels.rows

import org.itsadigitaltrust.hardwarelogger.models.HLModel.BatteryStatusModel
import org.itsadigitaltrust.hardwarelogger.services.HardwareGrabberService
import scalafx.beans.property.StringProperty

final class BatteryStatusTableRowViewModel(model: BatteryStatusModel)(using HardwareGrabberService) extends TableRowViewModel[BatteryStatusModel](model):
  def statusProperty: StringProperty =
    wrapper.field("status", _.status, "")(StringProperty.apply)
    
  def chargeProperty: StringProperty =
    wrapper.field("charge", _.charge.toString + "%", "")(StringProperty.apply)
    
  def healthProperty: StringProperty =
    wrapper.field("health", _.health.toString + "%", "")(StringProperty.apply)
    
  
  
