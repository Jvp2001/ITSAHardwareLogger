package org.itsadigitaltrust.hardwarelogger.services

import org.itsadigitaltrust.common.NetworkUtils

import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.services.OshiHardwareGrabberApplicationService.databaseService
import org.itsadigitaltrust.hardwarelogger.services.{HLDatabaseService, HLDatabaseServiceException, HardwareGrabberService, OshiHardwareGrabberService}
import org.itsadigitaltrust.hardwarelogger.tasks
import org.itsadigitaltrust.hardwarelogger.tasks.HardwareLoggerTaskContext

import oshi.SystemInfo
import ox.either.ok

trait OshiHardwareGrabberTestServiceTrait(using databaseService: HLDatabaseService) extends OshiHardwareGrabberService


final class OshiHardwareGrabberTestService(using databaseService: HLDatabaseService, context: HardwareLoggerTaskContext.WaitBackground) extends OshiHardwareGrabberTestServiceTrait(using databaseService):


  load()()
  override protected def findDriveIdBySerialNumber(serial: String)(using HardwareLoggerTaskContext.Background): Either[HLDatabaseServiceException, String] =
    ox.either:
      if ProgramMode.isInNormalMode && NetworkUtils.isConnected then
        databaseService.findItsaIdBySerialNumber(serial).ok()
      else
        databaseService.findWipingRecord(serial).map(_.itsaID).ok()

  override protected def findItsaIdBySerialNumber(serial: String)(using HardwareLoggerTaskContext.Background): Either[HLDatabaseServiceException, String] =
    databaseService.findItsaIdBySerialNumber(serial)

  
end OshiHardwareGrabberTestService
