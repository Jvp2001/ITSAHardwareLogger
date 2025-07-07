package org.itsadigitaltrust.hardwarelogger.services

import org.itsadigitaltrust.hardwarelogger.backend.entities.{DiskCreator, InfoCreator, MediaCreator, MemoryCreator, WipingCreator}
import org.itsadigitaltrust.hardwarelogger.models.{GeneralInfoModel, HardDriveModel, MemoryModel, ProcessorModel}
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{NotificationCentre, NotificationName}
import org.itsadigitaltrust.hardwarelogger.tasks.HardwareLoggerTask

import java.net.InterfaceAddress
import java.time.OffsetDateTime


object HLDatabaseTestService extends CommonHLDatabase[HardwareLoggerTask]:
  override def addWipingRecords(using itsaID: String) (drives: HardDriveModel*): Unit =
    val records = drives.zipWithIndex.map: (drive, index) =>
      WipingCreator(hddID = s"NO ID${index + 1}", serial = drive.serial, model = drive.model, insertionDate = OffsetDateTime.now, capacity = drive.size.toString, `type` = drive.`type`, description = "", health = drive.health.toByte, toUpdate = true , isSsd = true, formFactor = None)

    db.get.addWipingRecords(records*)
  

  override def executeTasks()(using notificationCentre: NotificationCentre[NotificationName])(using hardwareGrabberService: HardwareGrabberService): Unit =
    print("Executing tasks...")
    transactionQueue.forEach:
      case mediaCreator: MediaCreator => db.get.insertOrUpdate(mediaCreator)
      case infoCreator: InfoCreator => db.get.insertOrUpdate(infoCreator)
      case memoryCreator: MemoryCreator => db.get.insertOrUpdate(memoryCreator)
      case diskCreator: DiskCreator =>
        db.get.insertOrUpdate(diskCreator)
      case _ => scala.sys.error("Unknown type!")


    notificationCentre.post(NotificationName.DBSuccess)



