package org.itsadigitaltrust.hardwarelogger.services

object OshiHardwareGrabberTestService extends OshiHardwareGrabberService, TestServicesModule:
  override protected def findItsaIdBySerialNumber(serial: String): Option[String] =
    databaseService.findItsaIdBySerialNumber(serial)
  override def load(): Unit =
    loadProcessors()
    loadMemory()
    loadHardDrives()
    loadGeneralInfo()

