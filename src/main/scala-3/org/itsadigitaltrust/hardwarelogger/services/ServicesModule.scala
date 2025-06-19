package org.itsadigitaltrust.hardwarelogger.services



trait ServicesModule:
  given hardwareGrabberService: HardwareGrabberService = OshiHardwareGrabberApplicationService

  given hardwareIDValidationService: HardwareIDValidationService = new SimpleHardwareIDValidationService

  given notificationCentre: NotificationCentre[NotificationChannel] = SimpleNotificationCentre

  given databaseService: HLDatabaseService = SimpleHLDatabaseService
  
  given issueReporterService: IssueReporterService = StandardIssueReporterService()
  