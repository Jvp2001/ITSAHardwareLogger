package org.itsadigitaltrust.hardwarelogger.services

import org.itsadigitaltrust.common.NetworkUtils
import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.services.OshiHardwareGrabberApplicationService.databaseService
import org.itsadigitaltrust.hardwarelogger.services.{HLDatabaseService, HLDatabaseServiceException, HardwareGrabberService, OshiHardwareGrabberService}
import org.itsadigitaltrust.hardwarelogger.tasks
import org.scalatest.funsuite.AnyFunSuite
import oshi.SystemInfo
import ox.either.ok

trait OshiHardwareGrabberTestServiceTrait(using databaseService: HLDatabaseService) extends OshiHardwareGrabberService


final class OshiHardwareGrabberTestService(using databaseService: HLDatabaseService) extends OshiHardwareGrabberTestServiceTrait(using databaseService)
  
end OshiHardwareGrabberTestService



class OshiHardwareGrabberServiceTests extends AnyFunSuite with TestServicesModule:
  test("Get processor"):
    val option = hardwareGrabberService.processors
    assert(option.isRight && option.getOrElse(Nil).nonEmpty)

  test("Get memory"):
    val option = hardwareGrabberService.memory
    assert(option.isRight && option.getOrElse(Nil).nonEmpty)

  test("Get hard drives"):
    val option = hardwareGrabberService.hardDrives
    assert(option.isRight && option.getOrElse(Nil).nonEmpty)

  test("Get media"):
    val option = hardwareGrabberService.media
    assert(option.isRight && option.getOrElse(Nil).nonEmpty)


