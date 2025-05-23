package org.itsadigitaltrust.hardwarelogger.services

trait TestServicesModule:
  given databaseService: HLDatabaseService = HLDatabaseTestService
  given hardwareGrabberService: HardwareGrabberService = new OshiHardwareGrabberTestService
  given hardwareIDValidationService: HardwareIDValidationService = new SimpleHardwareIDValidationService
  given notificationCentre: NotificationCentre[NotificationChannel] = SimpleNotificationCentre

