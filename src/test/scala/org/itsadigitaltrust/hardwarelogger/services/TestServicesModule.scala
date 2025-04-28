package org.itsadigitaltrust.hardwarelogger.services

trait TestServicesModule:
  given hardwareGrabberService: HardwareGrabberService = OshiHardwareGrabberTestService
  given hardwareIDValidationService: HardwareIDValidationService = new SimpleHardwareIDValidationService
  given notificationCentre: NotificationCentre[NotificationChannel] = SimpleNotificationCentre
  given databaseService: HLDatabaseService = HLDatabaseTestService

