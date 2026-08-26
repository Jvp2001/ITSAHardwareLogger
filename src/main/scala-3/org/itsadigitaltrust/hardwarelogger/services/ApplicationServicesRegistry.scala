package org.itsadigitaltrust.hardwarelogger.services

import com.mysql.cj.jdbc.MysqlDataSource
import org.itsadigitaltrust.hardwarelogger.backend.database.{HLDatabase, MySQLHLDatabase}
import org.itsadigitaltrust.hardwarelogger.backend.{DBProperties, DataSourceLoader, DataSourceLoaderService}

trait HardwareGrabberModule:
  given hardwareGrabberService: HardwareGrabberService = OshiHardwareGrabberApplicationService.instance

trait DatabaseModule(using localhost: Boolean = false) extends HardwareGrabberModule:
  private given dataSourceLoader: DataSourceLoader = new DataSourceLoader

  private given backend: HLDatabase = new MySQLHLDatabase
  given databaseService: HLDatabaseService = MySQLHardwareLoggerDatabaseService()

trait HardwareIDValidationModule:
  given hardwareIDValidationService: HardwareIDValidationService = new SimpleHardwareIDValidationService

trait IssuesModule:
  given issueService: IssueReporterService = GitHubIssueReporterService()



trait ApplicationServicesRegistry extends HardwareGrabberModule, HardwareIDValidationModule, DatabaseModule, IssuesModule:
  this: org.itsadigitaltrust.hardwarelogger.HardwareLoggerApplication.type =>




