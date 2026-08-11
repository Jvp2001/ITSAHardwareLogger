package org.itsadigitaltrust.hardwarelogger.services

import com.mysql.cj.jdbc.MysqlDataSource
import org.itsadigitaltrust.hardwarelogger.backend.{DBProperties, DataSourceLoader, DataSourceLoaderService}

trait HardwareGrabberModule:
  given hardwareGrabberService: HardwareGrabberService = OshiHardwareGrabberApplicationService

trait DatabaseModule(using localhost: Boolean = false) extends HardwareGrabberModule:
  given dataSourceLoader: DataSourceLoader = new DataSourceLoader(using localhost)

  given databaseService: HLDatabaseService = MySQLHardwareLoggerDatabaseService()

trait HardwareIDValidationModule:
  given hardwareIDValidationService: HardwareIDValidationService = new SimpleHardwareIDValidationService

trait IssuesModule:
  given issueService: IssueReporterService = GitHubIssueReporterService()



trait ApplicationServicesRegistry extends HardwareGrabberModule, HardwareIDValidationModule, DatabaseModule, IssuesModule




