package org.itsadigitaltrust.hardwarelogger.services

import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{HLMessageArgs, HardwareLoggerNotificationCentre, HardwareNotificationArgType, NotificationCentreService, NotificationName}
import org.itsadigitaltrust.hardwarelogger.tasks.HardwareLoggerTaskContext

trait HardwareGrabberModule:
  given hardwareGrabberService: HardwareGrabberService = OshiHardwareGrabberApplicationService

trait HardwareIDValidationModule:
  given hardwareIDValidationService: HardwareIDValidationService = new SimpleHardwareIDValidationService

trait NotificationCentreModule:
  given notificationCentre: NotificationCentreService[NotificationName] = HardwareLoggerNotificationCentre
  type Message = notificationCentre.type#Message

trait DatabaseModule extends NotificationCentreModule, HardwareGrabberModule:
  import HardwareLoggerTaskContext.waitBackground
  given databaseService: HLDatabaseService = DatabaseModule.MySQLHardwareLoggerDatabaseService(using waitBackground)
end DatabaseModule
private object DatabaseModule:
  private[services] class MySQLHardwareLoggerDatabaseService(using HardwareLoggerTaskContext.WaitBackground) extends CommonHLDatabase
trait IssuesModule:
  given issueReporterService: IssueReporterService = GitHubIssueReporterService()

trait ApplicationServicesRegistry extends HardwareGrabberModule, HardwareIDValidationModule, NotificationCentreModule, DatabaseModule, IssuesModule




