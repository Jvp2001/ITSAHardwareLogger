package org.itsadigitaltrust.hardwarelogger.services



class OshiHardwareGrabberTestService(using db: HLDatabaseService) extends TestServicesModule, OshiHardwareGrabberService:
  override given dbService: HLDatabaseService = db
  
  override protected def findDriveIdBySerialNumber(serial: String): Option[String] =
    dbService.findItsaIdBySerialNumber(serial)

  

  override def load()(finished: () => Unit): Unit =
    loadProcessors()
    loadMemory()
    loadHardDrives()
    loadGeneralInfo()
    finished()

  override protected def findGeneralInfoByPCSerialNumber(serial: String): Option[String] = ???


