package org.itsadigitaltrust.hardwarelogger.services

import org.itsadigitaltrust.hardwarelogger.HardwareLoggerApplication
import org.itsadigitaltrust.hardwarelogger.models.{GeneralInfoModel, HLModel, HardDriveModel, MemoryModel, ProcessorModel}
import org.scalatest.funsuite.AnyFunSuite

import scala.reflect.ClassTag


class DatabaseServiceTests extends AnyFunSuite with TestServicesModule:
  extension (db: HLDatabaseService)
    def ++=[M <: HLModel : ClassTag](models: Seq[M]): Unit =
      models.foreach: model =>
        db += model

  def load(): Unit =
    hardwareGrabberService.load()
    databaseService.connect(HardwareLoggerApplication.getClass, "db/db.properties")
    databaseService.itsaId = "85977"
    notificationCentre.subscribe(NotificationChannel.DBSuccess)((_, _) => println("Database connection successful"))
  load()

  case class HardwareData(info: GeneralInfoModel, drives:  Seq[HardDriveModel], memory: Seq[MemoryModel], processors:Seq[ProcessorModel])
  def getHardwareData: HardwareData =
    HardwareData(info = hardwareGrabberService.generalInfo, drives = hardwareGrabberService.hardDrives, memory = hardwareGrabberService.memory, processors = hardwareGrabberService.processors)
    
  test("Find by id"):
    val hardwareData = getHardwareData
    val info = databaseService.findByID[GeneralInfoModel](hardwareData.info.itsaID.getOrElse(""))
    val drives = databaseService.findByID[HardDriveModel]()
    val memory = databaseService.findByID[MemoryModel]()
    val processors = databaseService.findByID[ProcessorModel]()

    assert(info.isDefined &&  drives.isDefined && memory.isDefined && processors.isDefined)
  test("Find all starting with id"):
    val hardwareData = getHardwareData
    val id = hardwareData.info.itsaID match
      case Some(value) if value == "" => "85977"
      case Some(value) => value
      case None => "85977"

    val info = databaseService.findAllStartingWithID[GeneralInfoModel](id)
    val drives = databaseService.findAllStartingWithID[HardDriveModel](id)
    val memory = databaseService.findAllStartingWithID[MemoryModel](id)
    val processors = databaseService.findAllStartingWithID[ProcessorModel](id)

    assert(info.nonEmpty &&  drives.nonEmpty && memory.nonEmpty && processors.nonEmpty)

  test("Mark all rows with ID as error"):
    val hardwareData = getHardwareData
    val id = hardwareData.info.itsaID match
      case Some(value) if value == "" => "85977"
      case Some(value) => value
      case None => "85977"

    databaseService.markAllRowsWithIDAsError[GeneralInfoModel](id)
    databaseService.markAllRowsWithIDAsError[HardDriveModel](id)
    databaseService.markAllRowsWithIDAsError[MemoryModel](id)
    databaseService.markAllRowsWithIDAsError[ProcessorModel](id)

    val info = databaseService.findByID[GeneralInfoModel](id)
    val drives = databaseService.findByID[HardDriveModel](id)
    val memory = databaseService.findByID[MemoryModel](id)
    val processors = databaseService.findByID[ProcessorModel](id)

    assert(info.isDefined &&  drives.isDefined && memory.isDefined && processors.isDefined)
  
  
  test("Mark and insert new"):
    val hardwareData = getHardwareData
    val id = hardwareData.info.itsaID match
      case Some(value) if value == "" => "85977"
      case Some(value) => value
      case None => "85977"

    databaseService.markAllRowsWithIDAsError[GeneralInfoModel](id)
    databaseService.markAllRowsWithIDAsError[HardDriveModel](id)
    databaseService.markAllRowsWithIDAsError[MemoryModel](id)
    databaseService.markAllRowsWithIDAsError[ProcessorModel](id)

    val info = databaseService.findByID[GeneralInfoModel](id)
    val drives = databaseService.findByID[HardDriveModel](id)
    val memory = databaseService.findByID[MemoryModel](id)
    val processors = databaseService.findByID[ProcessorModel](id)
    
    databaseService += hardwareData.info
    databaseService ++= hardwareData.drives
    databaseService ++= hardwareData.memory
    databaseService ++= hardwareData.processors
    

    assert(info.isDefined &&  drives.isDefined && memory.isDefined && processors.isDefined)

