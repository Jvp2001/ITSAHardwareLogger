package org.itsadigitaltrust.hardwarelogger.services


trait ServicesModule:
  given databaseService: HLDatabaseService = SimpleHLDatabaseService
  
  given hardwareGrabberService: HardwareGrabberService = new OshiHardwareGrabberApplicationService

  given hardwareIDValidationService: HardwareIDValidationService = new SimpleHardwareIDValidationService

  given notificationCentre: NotificationCentre[NotificationChannel] = SimpleNotificationCentre

