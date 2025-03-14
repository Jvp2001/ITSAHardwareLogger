package org.itsadigitaltrust.hardwarelogger.services

import org.springframework.stereotype.Service

@Service
trait HardwareGrabberService:
  import org.itsadigitaltrust.hardwarelogger.models.*
  def getGeneralInfo(): GeneralInfo

  def getHardDrives(): List[HardDrive]

  def getMemory(): List[Memory]

  def getProcessors(): List[Processor]

  def getMedia(): List[Media]
  
