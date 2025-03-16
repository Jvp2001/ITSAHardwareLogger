package org.itsadigitaltrust.hardwarelogger.services



trait HardwareGrabberService:

  import org.itsadigitaltrust.hardwarelogger.models.*

  def getGeneralInfo: GeneralInfo

  def getHardDrives: List[HardDrive]

  def getMemory: List[Memory]

  def getProcessors: List[Processor]

  def getMedia: List[Media]
  
