package org.itsadigitaltrust.hardwarelogger.services.database

import org.itsadigitaltrust.common.types.DataSizeType.{DataSize, DataSizeUnit}
import org.itsadigitaltrust.common.types.PercentageType.Percentage
import org.itsadigitaltrust.hardwarelogger.backend.database.HLDatabase
import org.itsadigitaltrust.hardwarelogger.backend.database.RepoToEntityCreatorGivens.given
import org.itsadigitaltrust.hardwarelogger.backend.entities.{DiskCreator, Wiping, WipingCreator}
import org.itsadigitaltrust.hardwarelogger.backend.{DataSourceLoaderService, HLDBErrorCode, HLSqlDataSource}
import org.itsadigitaltrust.hardwarelogger.backend.database.MySQLHLDatabase
import org.itsadigitaltrust.hardwarelogger.models.HLModel.{HardDriveConnectionType, HardDriveModel}
import org.itsadigitaltrust.hardwarelogger.services.database.mappers.HLModelToBackendMappers
import org.itsadigitaltrust.hardwarelogger.services.validation.GlobalValidationContext
import org.scalatest.funsuite.AnyFunSuite
import ox.Ox

import java.time.OffsetDateTime
import java.util.Properties
import java.util.UUID

class SaveConflictMySQLIntegrationTests extends AnyFunSuite:

//  private val integrationEnabled = sys.env.get("ITSA_MYSQL_INTEGRATION").contains("true")

  private val integrationEnabled = true

  test("MySQL persists HDD ID 85977.0 and can read it back"):
    assume(integrationEnabled, "Set ITSA_MYSQL_INTEGRATION=true to run against the configured local test database.")

    val backend = new MySQLHLDatabase(using localTestDataSourceLoader())
    given HLDatabase = backend
    val mapper = HLModelToBackendMappers.wipingToWipingCreator

    ox.supervised:
      assert(backend.connect().isRight, "Could not connect to the configured local test MySQL database.")
      var cleanupSerial: Option[String] = None
      try
        val existingID = backend.findFirstByItsaID[WipingCreator]("85977.0")
        val serial = existingID match
          case Right(existing) => existing.serial
          case Left(HLDBErrorCode.NoEntriesFound) =>
            val newSerial = s"85977${UUID.randomUUID().toString.replace("-", "").take(8)}"
            cleanupSerial = Some(newSerial)
            newSerial
          case Left(error) => fail(s"Could not check whether HDD ID 85977.0 exists: $error")

        val saved = mapper.executeSave(drive(serial, "85977.0"))
        assert(saved.isRight, s"Could not save HDD ID 85977.0 to MySQL: $saved")

        val bySerial = record(backend, serial)
        assert(bySerial.hddID == "85977.0", s"Saved row has an unexpected HDD ID: ${bySerial.hddID}")
        val byID = backend.findFirstByItsaID[WipingCreator]("85977.0")
        assert(byID.exists(row => row.serial == serial && row.hddID == "85977.0"), s"Could not read persisted HDD ID 85977.0: $byID")
      finally
        cleanupSerial.foreach: serial =>
          val cleanup = backend.deleteByAllSerials[WipingCreator](serial)
          assert(cleanup.isRight, s"Could not clean up live-write test row: $cleanup")

  test("MySQL returns the computer ITSA ID associated with a drive serial"):
    val backend = new MySQLHLDatabase(using localTestDataSourceLoader())
    given HLDatabase = backend
    val databaseService = new MySQLHardwareLoggerDatabaseService(using false, backend, localTestDataSourceLoader())
    val serial = s"DISK${UUID.randomUUID().toString.replace("-", "").take(12)}"

    ox.supervised:
      assert(backend.connect().isRight, "Could not connect to the configured local test MySQL database.")
      try
        val inserted = backend.insert[DiskCreator](
          DiskCreator("85977.0", "integration-test-drive", serial, "1 GB", "SATA", "integration test")
        )
        assert(inserted.isRight, s"Could not insert test disk association: $inserted")
        assert(databaseService.findDiskIDBySerialNumber(serial) == Right(Some("85977.0")))
      finally
        val cleanup = backend.deleteByAllSerials[DiskCreator](serial)
        assert(cleanup.isRight, s"Could not clean up test disk association: $cleanup")

  test("MySQL wiping lookup and update ignore surrounding serial whitespace"):
    val backend = new MySQLHLDatabase(using localTestDataSourceLoader())
    given HLDatabase = backend
    val serial = s"PADDED${UUID.randomUUID().toString.replace("-", "").take(12)}"
    val hddID = s"85977.0${UUID.randomUUID().toString.replace("-", "").take(4).map(c => ('A' + Character.digit(c, 16)).toChar)}A"
    val existingID = s"${hddID}B"
    val mapper = HLModelToBackendMappers.wipingToWipingCreator

    ox.supervised:
      assert(backend.connect().isRight, "Could not connect to the configured local test MySQL database.")
      try
        val inserted = backend.insert[WipingCreator](
          WipingCreator(existingID, s"  $serial  ", "integration-test-drive", OffsetDateTime.now(), Some("1 GB"), Some("SSD"), Some("integration test"), 100, true, true, None)
        )
        assert(inserted.isRight, s"Could not insert padded-serial test row: $inserted")

        val found = backend.findBySerialNumber[WipingCreator](serial)
        assert(found.exists(_.serial == s"  $serial  "), s"Could not find wiping row using normalized serial: $found")

        val validation = new GlobalValidationContext[HardDriveModel]
        HLModelToBackendMappers.hardDriveToDiskCreator.provideValidationContext(drive(serial, hddID))(using validation)
        assert(!validation.getIssues.exists(_.message.contains("does not exist in the wiping table")), validation.getIssues.mkString("; "))

        val saved = mapper.executeSave(drive(serial, hddID))
        assert(saved.isRight, s"Could not update wiping row using normalized serial: $saved")
        val updated = backend.findBySerialNumber[WipingCreator](serial)
        assert(updated.exists(_.hddID == hddID), s"Wiping ID was not updated for normalized serial: $updated")
      finally
        val cleanup = backend.deleteByAllSerials[WipingCreator](serial)
        assert(cleanup.isRight, s"Could not clean up padded-serial test row: $cleanup")

  test("MySQL persists wiping conflicts, updates, and error suffixes"):
    assume(integrationEnabled, "Set ITSA_MYSQL_INTEGRATION=true to run against the configured local test database.")

    val backend = new MySQLHLDatabase(using localTestDataSourceLoader())
    given HLDatabase = backend
    val mapper = HLModelToBackendMappers.wipingToWipingCreator
    val suffix = UUID.randomUUID().toString.replace("-", "").map(c => ('A' + Character.digit(c, 16)).toChar)
    val idA = s"85977.0${suffix}A"
    val idB = s"85977.0${suffix}B"
    val idC = s"85977.0${suffix}C"
    val idD = s"85977.0${suffix}D"
    val idE = s"85977.0${suffix}E"
    val serialNew = s"T${suffix}N"
    val serialOwner = s"T${suffix}O"
    val serialCurrent = s"T${suffix}C"
    val serialArchived = s"T${suffix}A"
    val serialOwner2 = s"T${suffix}D"
    val testSerials = Seq(serialNew, serialOwner, serialCurrent, serialArchived, serialOwner2)

    ox.supervised:
      assert(backend.connect().isRight, "Could not connect to the configured local test MySQL database.")
      try
        val initial = WipingCreator(
          hddID = idA,
          serial = serialOwner,
          model = "integration-test-drive",
          insertionDate = OffsetDateTime.now(),
          capacity = Some("1 GB"),
          `type` = Some("SSD"),
          description = Some("integration test"),
          health = 100,
          toUpdate = true,
          isSsd = true,
          formFactor = None
        )
        val initialSave = backend.insert[WipingCreator](initial)
        assert(initialSave.isRight, s"Could not seed SQL conflict record: $initialSave")
        val initialRead = backend.findFirstByItsaID[WipingCreator](idA)
        assert(initialRead.isRight, s"Could not read SQL conflict record: $initialRead")
        val serialLookup = backend.findBySerialNumber[WipingCreator](serialNew)
        assert(serialLookup == Left(HLDBErrorCode.NoEntriesFound), s"Unexpected SQL serial lookup result: $serialLookup")
        val missingIDLookup = backend.findItsaIDBySerialNumber[WipingCreator](serialNew)
        assert(missingIDLookup == Left(HLDBErrorCode.NoEntriesFound), s"Unexpected SQL missing-ID lookup result: $missingIDLookup")

        val duplicateWarnings = validate(drive(serialNew, idA))
        assert(duplicateWarnings.exists(_.contains("already in use")), duplicateWarnings.mkString("; "))
        val prefixedRows = backend.findAllStartingWithItsaID[WipingCreator](idA)
        assert(prefixedRows.isRight, s"Could not find rows by HDD ID prefix: $prefixedRows")
        val duplicateSave = mapper.executeSave(drive(serialNew, idA))
        assert(duplicateSave.isRight, s"Could not save duplicate HDD ID: $duplicateSave")
        assert(record(backend, serialOwner).hddID == s"$idA-E1")
        assert(record(backend, serialNew).hddID == idA)

        val currentInsert = mapper.executeSave(drive(serialCurrent, idB))
        assert(currentInsert.isRight, s"Could not insert current-serial SQL row: $currentInsert")
        val currentRead = backend.findBySerialNumber[WipingCreator](serialCurrent)
        assert(currentRead.isRight, s"Could not read current-serial SQL row: $currentRead")
        val changedIDWarnings = validate(drive(serialCurrent, idC))
        assert(changedIDWarnings.exists(_.contains("already recorded")), changedIDWarnings.mkString("; "))
        val currentUpdate = mapper.executeSave(drive(serialCurrent, idC))
        assert(currentUpdate.isRight, s"Could not update current-serial SQL row: $currentUpdate")
        assert(record(backend, serialCurrent).hddID == idC)

        val secondOwnerInsert = mapper.executeSave(drive(serialOwner2, idD))
        assert(secondOwnerInsert.isRight, s"Could not insert second SQL owner row: $secondOwnerInsert")
        val archivedInsert = mapper.executeSave(drive(serialArchived, s"$idD-E1"))
        assert(archivedInsert.isRight, s"Could not insert pre-archived SQL row: $archivedInsert")
        val reassignmentSerial = s"$serialCurrent-REASSIGN"
        val reassignmentInsert = mapper.executeSave(drive(reassignmentSerial, idE))
        assert(reassignmentInsert.isRight, s"Could not insert reassignment SQL row: $reassignmentInsert")
        val reassignmentWarnings = validate(drive(reassignmentSerial, idD))
        assert(reassignmentWarnings.exists(_.contains("already in use")), reassignmentWarnings.mkString("; "))
        val reassignmentSave = mapper.executeSave(drive(reassignmentSerial, idD))
        assert(reassignmentSave.isRight, s"Could not save reassigned SQL row: $reassignmentSave")
        assert(record(backend, serialOwner2).hddID == s"$idD-E2")
        assert(record(backend, reassignmentSerial).hddID == idD)
      finally
        val cleanup = backend.deleteByAllSerials[WipingCreator]((testSerials :+ s"$serialCurrent-REASSIGN")*)
        assert(cleanup.isRight, s"Could not clean up test wiping rows: $cleanup")

  private def validate(model: HardDriveModel)(using Ox, HLDatabase): List[String] =
    val context = new GlobalValidationContext[HardDriveModel]
    val mapper = HLModelToBackendMappers.wipingToWipingCreator
    mapper.provideValidationContext(model)(using context)
    context.getIssues.map(_.message)

  private def record(backend: HLDatabase, serial: String)(using Ox): Wiping =
    backend.findBySerialNumber[WipingCreator](serial) match
      case Right(value) => value
      case Left(error) => fail(s"Could not reload wiping row for '$serial': $error")

  private def drive(serial: String, id: String): HardDriveModel =
    HardDriveModel(
      health = Percentage(100),
      performance = Percentage(100),
      size = DataSize(1, DataSizeUnit.GB),
      model = "integration-test-drive",
      serial = serial,
      connectionType = HardDriveConnectionType.SATA,
      itsaID = id
    )

  private def localTestDataSourceLoader(): DataSourceLoaderService[HLSqlDataSource] =
    val properties = new Properties()
    val input = Option(getClass.getResourceAsStream("/org/itsadigitaltrust/hardwarelogger/db/db.properties"))
      .getOrElse(fail("Missing test database configuration resource."))
    try properties.load(input)
    finally input.close()

    val database = required(properties, "db.name")
    val port = required(properties, "db.port").toInt
    val username = required(properties, "db.username")
    val password = required(properties, "db.password")
    val source = new HLSqlDataSource
    source.setURL(s"jdbc:mysql://localhost:$port/$database?useSSL=false&serverTimezone=UTC")
    source.setDatabaseName(database)
    source.setUser(username)
    source.setPassword(password)
    source.setLoginTimeout(5)

    new DataSourceLoaderService[HLSqlDataSource]:
      override lazy val dataSource: Either[HLDBErrorCode, HLSqlDataSource] = Right(source)

  private def required(properties: Properties, key: String): String =
    Option(properties.getProperty(key)).getOrElse(fail(s"Missing '$key' in the test database configuration."))
