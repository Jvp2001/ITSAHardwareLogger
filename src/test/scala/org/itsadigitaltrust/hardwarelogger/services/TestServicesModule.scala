package org.itsadigitaltrust.hardwarelogger.services

//import org.itsadigitaltrust.hardwarelogger.services.database.HLDatabaseService
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{HardwareLoggerNotificationCentre, NotificationCentre, NotificationName}


trait TestServicesModule:
//  given databaseService: HLDatabaseService = HLDatabaseTestService
  given hardwareGrabberService: HardwareGrabberService = OshiHardwareGrabberTestService
  given hardwareIDValidationService: HardwareIDValidationService = new SimpleHardwareIDValidationService
  given notificationCentre: NotificationCentre[NotificationName] = HardwareLoggerNotificationCentre
  

