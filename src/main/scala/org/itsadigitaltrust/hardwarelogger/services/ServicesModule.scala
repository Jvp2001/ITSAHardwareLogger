package org.itsadigitaltrust.hardwarelogger.services


trait ServicesModule:
  given hardwareGrabberService: HardwareGrabberService = OshiHardwareGrabberService

  given hardwareIDValidationService: HardwareIDValidationService = new SimpleHardwareIDValidationService

  given notificationCentre: NotificationCentre[NotificationChannel] = SimpleNotificationCentre

  given databaseService: HLDatabaseService = SimpleHLDatabaseService
