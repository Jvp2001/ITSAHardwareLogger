package org.itsadigitaltrust.hardwarelogger.traits

import org.itsadigitaltrust.hardwarelogger.models.HLModel.GeneralInfoModel
import org.itsadigitaltrust.hardwarelogger.services.HardwareGrabberModule

trait ModelGeneratorFromOshi extends HardwareGrabberModule:
  def generateGeneralInfo(itsaID: String): GeneralInfoModel =
    hardwareGrabberService.generalInfo  
    
