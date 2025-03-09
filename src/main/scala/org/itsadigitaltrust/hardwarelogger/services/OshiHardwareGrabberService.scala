package org.itsadigitaltrust.hardwarelogger.services

import org.itsadigitaltrust.hardwarelogger.models.{GeneralInfo, HardDrive, Media, Memory, Processor}

class OshiHardwareGrabberService extends HardwareGrabberService:
  override def getGeneralInfo(): GeneralInfo = ???

  override def getHardDrives(): List[HardDrive] = ???

  override def getMemory(): List[Memory] = ???

  override def getProcessors(): List[Processor] = ???

  override def getMedia(): List[Media] = ???
  
