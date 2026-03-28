package org.itsadigitaltrust.hardwarelogger.services

//import org.itsadigitaltrust.hardwarelogger.services.database.HLDatabaseService
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{HardwareLoggerNotificationCentre, NotificationCentreService, NotificationName}
import org.itsadigitaltrust.hardwarelogger.services.{HLDatabaseService, HardwareGrabberService, HardwareIDValidationService}


trait TestServicesModule:
  given databaseService: HLDatabaseService = HLDatabaseServiceTestService()
  given hardwareGrabberService: HardwareGrabberService = new OshiHardwareGrabberTestService
  given hardwareIDValidationService: HardwareIDValidationService = new SimpleHardwareIDValidationService
  given notificationCentre: NotificationCentreService[NotificationName] = HardwareLoggerNotificationCentre
  

