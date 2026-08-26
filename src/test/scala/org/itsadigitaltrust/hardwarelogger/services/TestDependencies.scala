package org.itsadigitaltrust.hardwarelogger.services

import com.mysql.cj.jdbc.MysqlDataSource
import org.itsadigitaltrust.hardwarelogger.backend.database.{HLDatabase, MySQLHLDatabase}
import org.itsadigitaltrust.hardwarelogger.backend.{DataSourceLoader, DataSourceLoaderService, HLSqlDataSource}
import org.itsadigitaltrust.hardwarelogger.services.{DatabaseTestService, HLDatabaseService, HardwareIDValidationService, OshiHardwareGrabberService, OshiHardwareGrabberTestService, SimpleHardwareIDValidationService}
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState

trait TestDependencies:
  given useLocalhost: Boolean = true


  
  private final given dataSourceLoader: DataSourceLoaderService[HLSqlDataSource] = DataSourceLoader(dbName = "hwloggertesting")
  private final given backend: HLDatabase = new MySQLHLDatabase
  final protected given databaseService: DatabaseTestService = new DatabaseTestService

  final protected given idValidationService: IDValidationTestService = new IDValidationTestService
  
  final given hardwareGrabberService: OshiHardwareGrabberService = new OshiHardwareGrabberTestService

  protected lazy val applicationState: HardwareLoggerApplicationState = new HardwareLoggerApplicationState


  given hardwareIDValidationService: HardwareIDValidationService = new SimpleHardwareIDValidationService

  protected type DatabaseError = databaseService.type#Error
  protected type HardwareGrabberError = hardwareGrabberService.type#Error


  import databaseService.given
end TestDependencies

