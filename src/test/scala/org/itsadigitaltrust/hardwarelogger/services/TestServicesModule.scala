package org.itsadigitaltrust.hardwarelogger.services

import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{NotificationCentre, NotificationName, SimpleNotificationCentre}


trait TestServicesModule:
  given databaseService: HLDatabaseService = HLDatabaseTestService
  given hardwareGrabberService: HardwareGrabberService = OshiHardwareGrabberTestService
  given hardwareIDValidationService: HardwareIDValidationService = new SimpleHardwareIDValidationService
  given notificationCentre: NotificationCentre[NotificationName] = SimpleNotificationCentre
  

