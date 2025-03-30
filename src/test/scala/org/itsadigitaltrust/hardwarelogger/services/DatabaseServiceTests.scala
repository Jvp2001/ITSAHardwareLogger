package org.itsadigitaltrust.hardwarelogger.services

import org.itsadigitaltrust.hardwarelogger.HardwareLoggerApplication
import org.itsadigitaltrust.hardwarelogger.models.HLModel
import org.scalatest.funsuite.AnyFunSuite


class DatabaseServiceTests extends AnyFunSuite with ServicesModule:
  extension (db: HLDatabaseService)
    def ++=[M <: HLModel](models: Seq[M]): Unit =
      models.foreach: model =>
        db += model

  def load(): Unit =
    hardwareGrabberService.load()
    databaseService.connect(HardwareLoggerApplication.getClass, "db/db.properties")
    databaseService.itsaid = "85977"
    notificationCentre.subscribe(NotificationChannel.DBSuccess)(onDBSuccess)
  load()


  var times = 0
  def onDBSuccess(key: NotificationChannel, args: Seq[Any]): Unit =
    times += 1

  def queueData(): Unit =
    databaseService ++= hardwareGrabberService.memory
    databaseService ++= hardwareGrabberService.processors
    databaseService ++= hardwareGrabberService.hardDrives
    databaseService ++= hardwareGrabberService.media
    databaseService += hardwareGrabberService.generalInfo



  test("Concurrency Test"):
    queueData()
    assert(times == 1)


