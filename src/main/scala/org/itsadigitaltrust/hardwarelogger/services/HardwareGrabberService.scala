package org.itsadigitaltrust.hardwarelogger.services



trait HardwareGrabberService:

  import org.itsadigitaltrust.hardwarelogger.models.*

  def getGeneralInfo: GeneralInfoModel

  def getHardDrives: List[HardDriveModel]

  def getMemory: List[MemoryModel]

  def getProcessors: List[ProcessorModel]

  def getMedia: List[MediaModel]
  
