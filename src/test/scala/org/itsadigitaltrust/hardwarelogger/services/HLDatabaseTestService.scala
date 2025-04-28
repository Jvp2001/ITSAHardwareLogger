package org.itsadigitaltrust.hardwarelogger.services

import org.itsadigitaltrust.hardwarelogger.backend.entities.{DiskCreator, InfoCreator, MediaCreator, MemoryCreator}
import org.itsadigitaltrust.hardwarelogger.models.{GeneralInfoModel, HardDriveModel, MemoryModel, ProcessorModel}
import org.itsadigitaltrust.hardwarelogger.tasks.HardwareLoggerTask


object HLDatabaseTestService extends CommonHLDatabase[HardwareLoggerTask]:
  override def executeTasks()(using notificationCentre: NotificationCentre[NotificationChannel])(using hardwareGrabberService: HardwareGrabberService): Unit =
    print("Executing tasks...")
    transactionQueue.forEach:
      case mediaCreator: MediaCreator => db.get.insertOrUpdate(mediaCreator)
      case infoCreator: InfoCreator => db.get.insertOrUpdate(infoCreator)
      case memoryCreator: MemoryCreator => db.get.insertOrUpdate(memoryCreator)
      case diskCreator: DiskCreator =>
        db.get.insertOrUpdate(diskCreator)
      case _ => scala.sys.error("Unknown type!")


    notificationCentre.publish(NotificationChannel.DBSuccess)


