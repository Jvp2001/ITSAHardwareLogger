package org.itsadigitaltrust.hardwarelogger.services

import org.itsadigitaltrust.common.Operators.{or, |>}

import org.itsadigitaltrust.hardwarelogger.backend.HLDatabase.Error.ConnectionError
import org.itsadigitaltrust.hardwarelogger.backend.entities.*
import org.itsadigitaltrust.hardwarelogger.backend.{DataSourceLoaderFactory, DataSourceLoaderFactoryTests, DataSourceLoaderService, DataSourceLoaderServiceTest, HLDBErrorCode, HLDatabase}
import org.itsadigitaltrust.hardwarelogger.models.*
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{DBSuccessArgs, NotificationCentreService, NotificationName}
import org.itsadigitaltrust.hardwarelogger.services.{CommonHLDatabase, HLDatabaseServiceException}
import org.itsadigitaltrust.hardwarelogger.tasks.{HardwareLoggerTask, HardwareLoggerTaskContext}

import org.scalatest.funsuite.AnyFunSuite
import ox.either.{fail, ok}

import java.net.InterfaceAddress
import java.time.OffsetDateTime
import scala.reflect.ClassTag

private given dataSourceLoaderFactory: DataSourceLoaderFactory = DataSourceLoaderFactoryTests()

trait HLDatabaseServiceTestHelpersTrait
trait HLDatabaseServiceTestServiceTrait extends CommonHLDatabase:
  itsaID = "85977.0"

  override lazy val dbPropertiesFile: String =
    """
      |db.username=itsa
      |db.password=itsa123
      |db.name=hwlogger
      |db.port=3306
      |db.unit7.address=10.0.1.230
      |db.unit9.address=192.168.100.230
      |db.local.address=localhost
      |
      |#Time in seconds
      |db.timeout=1
      |
      |db.autoReconnect=true
      |db.maxReconnects=3
      |db.useLocalhost=true
      |db.localhost.username=root
      |db.localhost.password=root
      |""".stripMargin



  override def connect()(using context: HardwareLoggerTaskContext.WaitBackground): Either[HLDatabaseServiceException, HLDatabase] =
    import HLDatabaseService.given
    given dataSourceLoader: DataSourceLoaderService = DataSourceLoaderServiceTest("")
    ox.either:
      val dbValue = new HLDatabase(dbPropertiesFile)(using dataSourceLoader)
      dbValue
      
  end connect

  override def addWipingRecords(itsaID: String, drives: HardDriveModel*)(using context: HardwareLoggerTaskContext.WaitBackground): Either[HLDatabaseServiceException, Unit] =
    val records = drives.zipWithIndex.map: (drive, index) =>
      WipingCreator(hddID = s"NO ID${index + 1}", serial = drive.serial, model = drive.model, insertionDate = OffsetDateTime.now, capacity = drive.size.toString, `type` = drive.`type`, description = "", health = drive.health.toByte, toUpdate = true , isSsd = true, formFactor = None)
    ox.either[HLDatabaseServiceException, Unit]:
      db.match
        case Left(error) => error.fail()
        case Right(dbValue: HLDatabase) => dbValue.addWipingRecords(records*).getOrElse(HLDatabaseServiceException.unknownError())




end HLDatabaseServiceTestServiceTrait

final class HLDatabaseServiceTestService extends HLDatabaseServiceTestServiceTrait


class HLDatabaseTestService extends AnyFunSuite with TestServicesModule:
  import org.itsadigitaltrust.common.Operators.*

  import HLDatabaseTestService.*


  // connect to db

  databaseService.connect()

  //should fail if trying to addForegroundTask two PC with the same serial number to DB
  test("Should Fail to addForegroundTask PC with same serial number to DB"):
    val generalInfo = hardwareGrabberService.generalInfo.copy(itsaID = databaseService.itsaID.toOption)
    if databaseService.doesPCRecordExist(generalInfo) then
      assert(true)
    val result = databaseService.addPCRecord(generalInfo)
    val result2 = databaseService.addPCRecord(generalInfo)
    assert(result.isRight && result2.isLeft || result.isLeft && result2.isLeft)

object HLDatabaseTestService extends TestServicesModule:
  @main
  def HLDatabaseTestServiceCode: Unit =
    val generalInfo = hardwareGrabberService.generalInfo.copy(itsaID = databaseService.itsaID |> Option[String])

    if databaseService.findItsaIdBySerialNumber(generalInfo.serial).isRight then
      print("Already exists!")
    else
      val result = databaseService.addPCRecord(generalInfo)
      val result2 = databaseService.addPCRecord(generalInfo)








