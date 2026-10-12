package org.itsadigitaltrust.hardwarelogger.services.database

import org.itsadigitaltrust.common.types.DataSizeType.{DataSize, DataSizeUnit}
import org.itsadigitaltrust.common.types.PercentageType.Percentage
import org.itsadigitaltrust.hardwarelogger.backend.database.{EntityCreatorToEntityMapper, HLDatabase, RepoFromEntityCreator, UpdateAndInsert}
import org.itsadigitaltrust.hardwarelogger.backend.entities.*
import org.itsadigitaltrust.hardwarelogger.backend.{DataSourceLoaderService, HLDBErrorCode, HLDBTransactionResult, HLSqlDataSource}
import org.itsadigitaltrust.hardwarelogger.models.HLModel.{HardDriveConnectionType, HardDriveModel}
import org.itsadigitaltrust.hardwarelogger.services.database.mappers.HLModelToBackendMappers
import org.itsadigitaltrust.hardwarelogger.services.validation.GlobalValidationContext
import org.itsadigitaltrust.hardwarelogger.services.database.HLDatabaseServiceCoordinatorOperationContextDefaults.hardDriveModeContext
import org.itsadigitaltrust.hardwarelogger.services.OshiHardwareGrabberTestService
import org.itsadigitaltrust.hardwarelogger.state.filterBySerial
import org.scalatest.funsuite.AnyFunSuite
import org.itsadigitaltrust.hardwarelogger.backend.database.RepoToEntityCreatorGivens.given
import ox.Ox

import java.time.OffsetDateTime
import scala.collection.mutable
import scala.io.Source

class SaveConflictTests extends AnyFunSuite:
  private val absent = HLDBErrorCode.NoEntriesFound

  private def drive(serial: String, id: String): HardDriveModel =
    HardDriveModel(
      health = Percentage(100),
      performance = Percentage(100),
      size = DataSize(1, DataSizeUnit.GB),
      model = "test drive",
      serial = serial,
      connectionType = HardDriveConnectionType.SATA,
      itsaID = id
    )

  private def wiping(id: String, serial: String, rowID: Long): Wiping =
    Wiping(
      id = rowID,
      hddID = id,
      serial = serial,
      model = "test drive",
      insertionDate = OffsetDateTime.now(),
      capacity = Some("1 GB"),
      `type` = Some("SSD"),
      description = None,
      health = 100,
      toUpdate = true,
      isSsd = true,
      formFactor = None
    )

  private def save(db: InMemoryDatabase, model: HardDriveModel): Unit =
    val service = new MySQLHardwareLoggerDatabaseService(using false, db, InMemoryDatabase.loader)
    ox.supervised:
      val saved = service.executeFinalSave(model)(using hardDriveModeContext)
      assert(saved.isRight, s"Save failed: $saved")

  private def loadFakeDrive(db: InMemoryDatabase): HardDriveModel =
    val databaseService = new MySQLHardwareLoggerDatabaseService(using false, db, InMemoryDatabase.loader)
    val hardwareGrabber = new OshiHardwareGrabberTestService(using databaseService)
    val input = Option(getClass.getResourceAsStream("/FakeDrives.xml"))
      .getOrElse(fail("Missing FakeDrives.xml test fixture."))
    val source = Source.fromInputStream(input, "ISO-8859-2")
    val xml =
      try source.mkString
      finally source.close()
    ox.supervised:
      hardwareGrabber.loadHardDrives(xml) match
        case Right(drives) => drives.headOption.getOrElse(fail("FakeDrives.xml did not contain any drives."))
        case Left(error) => fail(s"Could not parse FakeDrives.xml: $error")

  private def warnings(db: InMemoryDatabase, model: HardDriveModel): List[String] =
    given HLDatabase = db
    val context = new GlobalValidationContext[HardDriveModel]
    val mapper = HLModelToBackendMappers.wipingToWipingCreator
    ox.supervised:
      mapper.provideValidationContext(model)(using context)
    context.getIssues.map(_.message)

  private def normalModeWarnings(db: InMemoryDatabase, model: HardDriveModel): List[String] =
    given HLDatabase = db
    val context = new GlobalValidationContext[HardDriveModel]
    val mapper = HLModelToBackendMappers.hardDriveToDiskCreator
    ox.supervised:
      mapper.provideValidationContext(model)(using context)
    context.getIssues.map(_.message)

  test("new serial and unused HDD ID create one wiping record"):
    val db = new InMemoryDatabase
    save(db, drive("serial-new", "100011.5"))
    assert(db.records.map(r => r.serial -> r.hddID) == Seq("serial-new" -> "100011.5"))

  test("saving HDD ID 85977.0 inserts a record that can be read back by serial and ID"):
    val db = new InMemoryDatabase
    val serial = "serial-save-85977"

    save(db, drive(serial, "85977.0"))

    ox.supervised:
      val bySerial = db.findBySerialNumber[WipingCreator](serial)
      assert(bySerial.exists(record => record.hddID == "85977.0" && record.serial == serial), s"Save was not persisted by serial: $bySerial")

      val byID = db.findFirstByItsaID[WipingCreator]("85977.0")
      assert(byID.exists(record => record.hddID == "85977.0" && record.serial == serial), s"Save was not persisted by HDD ID: $byID")
    assert(db.records.count(_.serial == serial) == 1, "Saving should create exactly one wiping record")

  test("saving HDD ID 85977.0 updates the existing serial record and persists the new ID"):
    val db = new InMemoryDatabase
    val serial = "serial-update-85977"
    db.records += wiping("100011.5", serial, 1)

    save(db, drive(serial, "85977.0"))

    ox.supervised:
      val bySerial = db.findBySerialNumber[WipingCreator](serial)
      assert(bySerial.exists(_.hddID == "85977.0"), s"Updated HDD ID was not persisted: $bySerial")
    assert(db.records.count(_.serial == serial) == 1, "Updating should not create a duplicate wiping record")

  test("normal-mode drive ID lookup returns the associated computer ITSA ID"):
    val db = new InMemoryDatabase
    val serial = "serial-linked-to-pc"
    db.diskRecords += Disk(1, "85977.0", "test drive", serial, "1 GB", "SATA", "")
    val service = new MySQLHardwareLoggerDatabaseService(using false, db, InMemoryDatabase.loader)

    ox.supervised:
      assert(service.findDiskIDBySerialNumber(serial) == Right(Some("85977.0")))
      assert(service.findDiskIDBySerialNumber("serial-without-disk") == Right(None))

  test("logging multiple unlogged FakeDrives assigns A, B IDs before error suffixes"):
    val db = new InMemoryDatabase
    val fakeDrive = loadFakeDrive(db)
    val drives = Seq(
      fakeDrive.copy(serial = s"${fakeDrive.serial}-TEST-1", itsaID = "85977.0"),
      fakeDrive.copy(serial = s"${fakeDrive.serial}-TEST-2", itsaID = "85977.0A"),
      fakeDrive.copy(serial = s"${fakeDrive.serial}-TEST-3", itsaID = "85977.0B"),
      fakeDrive.copy(serial = s"${fakeDrive.serial}-TEST-4", itsaID = "85977.0C")
    )
    db.records ++= Seq(
      wiping("85977.0", "serial-owner-base", 1),
      wiping("85977.0A", "serial-owner-a", 2),
      wiping("85977.0A-E1", "serial-archived-a", 3),
      wiping("85977.0B", "serial-owner-b", 4),
      wiping("85977.0C", "serial-owner-c", 5)
    )

    drives.foreach(save(db, _))

    assert(db.records.map(record => record.serial -> record.hddID).toSet == Set(
      "serial-owner-base" -> "85977.0-E1",
      "serial-owner-a" -> "85977.0A-E2",
      "serial-archived-a" -> "85977.0A-E1",
      "serial-owner-b" -> "85977.0B-E1",
      "serial-owner-c" -> "85977.0C-E1",
      drives(0).serial -> "85977.0",
      drives(1).serial -> "85977.0A",
      drives(2).serial -> "85977.0B",
      drives(3).serial -> "85977.0C"
    ))

  test("reload prefers an assigned HDD ID over the NOT LOGGED placeholder"):
    val unlogged = drive("serial-reload", "NOT LOGGED")
    val assigned = drive("serial-reload", "100011.5")
    assert(Some(Seq(unlogged, assigned)).filterBySerial.get.head.itsaID == "100011.5")
    assert(!HardDriveModel.hasLoggedItsaID("NOT LOGGED"))

  test("an unlogged hard drive produces the existing soft warning"):
    val messages = normalModeWarnings(new InMemoryDatabase, drive("serial-unlogged", "100011.5"))
    assert(messages.exists(_.contains("does not exist in the wiping table")))

  test("an HDD ID owned by another serial warns, archives the owner, then inserts"):
    val db = new InMemoryDatabase
    db.records += wiping("100011.5", "serial-owner", 1)
    assert(warnings(db, drive("serial-new", "100011.5")).exists(_.contains("already in use")))

    save(db, drive("serial-new", "100011.5"))

    assert(db.records.map(r => r.serial -> r.hddID).toSet == Set(
      "serial-owner" -> "100011.5-E1",
      "serial-new" -> "100011.5"
    ))

  test("changing the HDD ID for an existing serial updates its row without inserting another"):
    val db = new InMemoryDatabase
    db.records += wiping("100011.5", "serial-current", 1)
    assert(warnings(db, drive("serial-current", "200022.3")).exists(_.contains("already recorded")))

    save(db, drive("serial-current", "200022.3"))

    assert(db.records.map(r => r.serial -> r.hddID) == Seq("serial-current" -> "200022.3"))

  test("reassigning a conflicting HDD ID archives its owner and updates the current serial"):
    val db = new InMemoryDatabase
    db.records += wiping("100011.5", "serial-owner", 1)
    db.records += wiping("200022.3", "serial-current", 2)
    db.records += wiping("100011.5-E1", "serial-archived", 3)
    assert(warnings(db, drive("serial-current", "100011.5")).exists(_.contains("already in use")))

    save(db, drive("serial-current", "100011.5"))

    assert(db.records.map(r => r.serial -> r.hddID).toSet == Set(
      "serial-owner" -> "100011.5-E2",
      "serial-archived" -> "100011.5-E1",
      "serial-current" -> "100011.5"
    ))

  private final class InMemoryDatabase extends HLDatabase(using InMemoryDatabase.loader):
    val records: mutable.Buffer[Wiping] = mutable.ArrayBuffer.empty
    val diskRecords: mutable.Buffer[Disk] = mutable.ArrayBuffer.empty

    override def connect()(using Ox): Either[HLDBErrorCode, Unit] = Right(())

    override def findBySerialNumber[EC <: HLEntityCreatorWithSomeKindOfID](serialNumber: String)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[mapper.Out] =
      records.find(_.serial == serialNumber).map(_.asInstanceOf[mapper.Out]).toRight(absent)

    override def findAllStartingWithItsaID[EC <: HLEntityCreatorWithSomeKindOfID](itsaID: String)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Vector[mapper.Out]] =
      Right(records.filter(_.hddID.startsWith(itsaID)).map(_.asInstanceOf[mapper.Out]).toVector)

    override def findAllByItsaID[EC <: HLEntityCreatorWithSomeKindOfID](itsaID: String)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Vector[mapper.Out]] =
      Right(records.filter(_.hddID == itsaID).map(_.asInstanceOf[mapper.Out]).toVector)

    override def findFirstByItsaID[EC <: HLEntityCreatorWithSomeKindOfID](itsaID: String)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[mapper.Out] =
      records.find(_.hddID == itsaID).map(_.asInstanceOf[mapper.Out]).toRight(absent)

    override def replaceItsaID[EC <: HLEntityCreatorWithSomeKindOfID](oldItsaID: String, newItsaID: String)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Unit] =
      records.indices.filter(i => records(i).hddID == oldItsaID).foreach: i =>
        records(i) = records(i).copy(hddID = newItsaID)
      Right(())

    override def updateItsaIDBySerial[EC <: HLEntityCreatorWithSomeKindOfID](serial: String, newItsaID: String)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Unit] =
      records.indices.find(i => records(i).serial == serial).foreach: i =>
        records(i) = records(i).copy(hddID = newItsaID)
      Right(())

    override def findItsaIDBySerialNumber[EC <: HLEntityCreatorWithSomeKindOfID](serialNumber: String)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[String] =
      diskRecords.find(_.serial == serialNumber).map(_.itsaID)
        .orElse(records.find(_.serial == serialNumber).map(_.hddID))
        .toRight(absent)

    override def findAllByItsaIDEndingWith[EC <: HLEntityCreatorWithSomeKindOfID](suffix: String)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Vector[mapper.Out]] =
      Right(records.filter(_.hddID.endsWith(suffix)).map(_.asInstanceOf[mapper.Out]).toVector)

    override def insert[EC <: HLEntityCreatorWithSomeKindOfID](entityCreator: EC)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Unit] =
      entityCreator match
        case creator: WipingCreator =>
          records += Wiping(
            id = records.map(_.id).maxOption.getOrElse(0L) + 1,
            hddID = creator.hddID,
            serial = creator.serial,
            model = creator.model,
            insertionDate = creator.insertionDate,
            capacity = creator.capacity,
            `type` = creator.`type`,
            description = creator.description,
            health = creator.health,
            toUpdate = creator.toUpdate,
            isSsd = creator.isSsd,
            formFactor = creator.formFactor
          )
        case creator: DiskCreator =>
          diskRecords += Disk(
            id = diskRecords.map(_.id).maxOption.getOrElse(0L) + 1,
            itsaID = creator.itsaID,
            model = creator.model,
            serial = creator.serial,
            capacity = creator.capacity,
            `type` = creator.`type`,
            descr = creator.descr
          )
        case other => fail(s"Unexpected creator type: ${other.getClass.getName}")
      Right(())

    override def insertAll[EC <: HLEntityCreatorWithSomeKindOfID](entities: Seq[EC])(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Unit] =
      entities.foreach(insert(_))
      Right(())

    override def updateAndInsertAll[EC <: HLEntityCreatorWithSomeKindOfID : UpdateAndInsert as handler](entityCreators: EC*)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Unit] =
      Left(HLDBErrorCode.UnknownError("Not used by these mapper tests"))

    override def deleteByItsaID[EC <: HLEntityCreatorWithSomeKindOfID](itsaID: String)(using mapper: EntityCreatorToEntityMapper[EC], repoFromEntityCreator: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Unit] =
      Right(())

    override def deleteByAllSerials[EC <: HLEntityCreatorWithSomeKindOfID](serials: String*)(using mapper: EntityCreatorToEntityMapper[EC], repoFromEntityCreator: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Unit] =
      Right(())

  private object InMemoryDatabase:
    val loader: DataSourceLoaderService[HLSqlDataSource] = new DataSourceLoaderService[HLSqlDataSource]:
      override lazy val dataSource: Either[HLDBErrorCode, HLSqlDataSource] = Right(new HLSqlDataSource)
