package org.itsadigitaltrust.hardwarelogger.services

object OshiHardwareGrabberTestService extends OshiHardwareGrabberService, TestServicesModule:
  override protected def findDriveIdBySerialNumber(serial: String): Option[String] =
    databaseService.findItsaIdBySerialNumber(serial)

  

  override def load()(finished: () => Unit): Unit =
    loadProcessors()
    loadMemory()
    loadHardDrives()
    loadGeneralInfo()
    finished()

  override protected def findGeneralInfoByPCSerialNumber(serial: String): Option[String] = None

  override protected def findItsaIdBySerialNumber(serial: String): Option[String] = None


