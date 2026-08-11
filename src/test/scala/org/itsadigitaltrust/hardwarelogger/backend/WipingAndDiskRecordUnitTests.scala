package org.itsadigitaltrust.hardwarelogger.backend

import org.itsadigitaltrust.common.Operators.??
import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.services.TestServicesModule
import org.scalatest.funsuite.AnyFunSuite
import ox.either.fail

final class WipingAndDiskRecordTests extends AnyFunSuite with TestServicesModule:

  test("If a storage device is in the computer, a corresponding disk/wiping record should exist"):
    val result = ox.either:
      databaseService.connect() ?? fail("Could not connect to the database")

      val drive = hardwareGrabberService.hardDrives.map(_.headOption).getOrElse("No hard drives were discovered by the hardware grabber".fail()).get

      ProgramMode.mode = "HardDrive"
      val wipingRecord = databaseService.findWipingRecord(drive.serial)

      assert(wipingRecord.isRight)

    test("If a storage device is removed from the computer, stale disk/wiping records should be handled"):
      val result = ox.either:
        databaseService.connect()

        val itsaId = hardwareGrabberService.generalInfo.map(_.headOption.map(_.itsaID)).getOrElse("No general info found".fail()).get ?? ""

        databaseService.markAllRowsWithIDInDBAsError(itsaId)

      end result
    if result.isLeft then
      assert(false, result.getOrElse(""))
    assert(result.isRight && !result.isLeft)

  test("Saving a computer record should preserve matching drive identity data"):
    val result = ox.either:
      ProgramMode.mode = "Normal"
      databaseService.connect().fail()

      val info = hardwareGrabberService.generalInfo.map(_.headOption).getOrElse(hardwareGrabberService.LoadError("No general info found").fail())
      val drives = hardwareGrabberService.hardDrives.getOrElse(hardwareGrabberService.LoadError("No hard drives found").fail())

      val pcResult = databaseService.addPCRecord(info.get)
      val driveResult = if drives.nonEmpty then databaseService.addWipingRecords(info.get.itsaID.getOrElse(""), drives*) else Right(())
      pcResult.isRight || driveResult.isLeft && driveResult.isRight && pcResult.isRight
    assert(result.isRight && result.getOrElse(false))

end WipingAndDiskRecordTests
