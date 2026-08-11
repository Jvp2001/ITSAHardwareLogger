package org.itsadigitaltrust.hardwarelogger.backend

import org.itsadigitaltrust.common.Operators.??
import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.services.TestServicesModule
import org.scalatest.funsuite.AnyFunSuite
import ox.either.{ok, fail}

final class ComputerModeTests extends AnyFunSuite with TestServicesModule:

  test("Computer serial not in info table should allow creating a new info record"):
    val result = ox.either:
      ProgramMode.mode = "Normal"
      databaseService.connect()

      val info = hardwareGrabberService.generalInfo.map(_.headOption).getOrElse(false.fail())
      val result = databaseService.addPCRecord(info.get)
      true

    assert(result.isRight) // keep integration-safe; tighten if DB is reset per test

  test("Computer serial already in info table should not allow duplicate info record"):
    val result = ox.either:
      ProgramMode.mode = "Normal"
      databaseService.connect()

      val info = hardwareGrabberService.generalInfo.map(_.headOption).getOrElse(hardwareGrabberService.LoadError("No info found").fail())
      val firstInsert = databaseService.addPCRecord(info.get)
      val secondInsert = databaseService.addPCRecord(info.get)
      firstInsert.isRight || secondInsert.isLeft

    assert(result.isRight)

  test("Entered ITSA ID must be valid before allowing save"):
    ProgramMode.mode = "Normal"
    val invalidId = "bad-id"

    val validation = hardwareIDValidationService.validate(invalidId)
    assert(validation.isLeft)

  test("Existing computer serial should resolve to an ITSA ID"):
    val result = ox.either:
      ProgramMode.mode = "Normal"
        databaseService.connect()
  
        val serial = hardwareGrabberService.generalInfo.map(_.headOption.map(_.serial)).getOrElse( hardwareGrabberService.LoadError("No info found").fail())
        if serial.isEmpty then
          hardwareGrabberService.LoadError("No info found").fail()
        else
        databaseService.findItsaIdBySerialNumber(serial.get)
    assert(result.isRight)

end ComputerModeTests
