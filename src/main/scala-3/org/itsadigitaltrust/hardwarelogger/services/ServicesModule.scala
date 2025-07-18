package org.itsadigitaltrust.hardwarelogger.services

import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{NotificationCentre, NotificationName, HardwareLoggerNotificationCentre}

trait HardwareGrabberModule:
  given hardwareGrabberService: HardwareGrabberService = OshiHardwareGrabberApplicationService

trait HardwareIDValidationModule:
  given hardwareIDValidationService: HardwareIDValidationService = new SimpleHardwareIDValidationService

trait NotificationCentreModule:
  given notificationCentre: NotificationCentre[NotificationName] = HardwareLoggerNotificationCentre

trait DatabaseModule:
  given databaseService: HLDatabaseService = SimpleHLDatabaseService()

trait IssuesModule:
  given issueReporterService: IssueReporterService = StandardIssueReporterService()

trait ServicesModule extends HardwareGrabberModule, HardwareIDValidationModule, NotificationCentreModule, DatabaseModule, IssuesModule




