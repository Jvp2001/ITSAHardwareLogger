package org.itsadigitaltrust.hardwarelogger.services

import com.mysql.cj.jdbc.MysqlDataSource
import org.itsadigitaltrust.hardwarelogger.backend.database.{HLDatabase, MySQLHLDatabase}
import org.itsadigitaltrust.hardwarelogger.backend.{DBProperties, DataSourceLoader, DataSourceLoaderService}
import org.itsadigitaltrust.hardwarelogger.contextbasedemnubar.MenuManagerService
import org.itsadigitaltrust.hardwarelogger.services.database.HLDatabaseServiceExceptionBase
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState
import qupath.ui.logviewer.api.manager.LoggerManager
import qupath.ui.logviewer.logging.logback.LogbackManager

trait HardwareGrabberModule:
  given hardwareGrabberService: HardwareGrabberService = OshiHardwareGrabberApplicationService


trait DatabaseModule(using localhost: Boolean = false) extends HardwareGrabberModule:
  private given dbProps: DBProperties = DBProperties.default.copy(
//    name = "hwlogger",
    //fallbackAddress = "192.168.1.155",

  )
  import ox.either.orThrow
  private given dataSourceLoader: DataSourceLoader = 
    DataSourceLoader().left.map(HLDatabaseServiceException(_)).orThrow

  
  private given backend: HLDatabase = new MySQLHLDatabase
  given databaseService: HLDatabaseService = MySQLHardwareLoggerDatabaseService()

trait HardwareIDValidationModule:
  given hardwareIDValidationService: HardwareIDValidationService = new SimpleHardwareIDValidationService

trait IssuesModule:
  given issueService: IssueReporterService = GitHubIssueReporterService()

trait LoggerManagerModule:
  given loggerManager: LoggerManager = new LogbackManager

trait MenuManagerModule:
  given menuManagerService: MenuManagerService = new MenuManagerService

trait ApplicationServicesRegistry extends HardwareGrabberModule, HardwareIDValidationModule,
  DatabaseModule, IssuesModule, LoggerManagerModule, MenuManagerModule:
  this: org.itsadigitaltrust.hardwarelogger.HardwareLoggerApplication.type =>
  given applicationState: HardwareLoggerApplicationState = HardwareLoggerApplicationState()





