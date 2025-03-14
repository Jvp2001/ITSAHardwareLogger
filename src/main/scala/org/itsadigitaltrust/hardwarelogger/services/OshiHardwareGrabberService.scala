package org.itsadigitaltrust.hardwarelogger.services

import org.itsadigitaltrust.hardwarelogger.models.*
import org.springframework.stereotype.Service

@Service
class OshiHardwareGrabberService extends HardwareGrabberService:
  override def getGeneralInfo(): GeneralInfo = ???

  override def getHardDrives(): List[HardDrive] = ???

  override def getMemory(): List[Memory] = ???

  override def getProcessors(): List[Processor] = ???

  override def getMedia(): List[Media] = ???
  
