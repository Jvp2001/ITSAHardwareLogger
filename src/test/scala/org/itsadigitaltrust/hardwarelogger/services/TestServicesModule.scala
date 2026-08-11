package org.itsadigitaltrust.hardwarelogger.services

//import org.itsadigitaltrust.hardwarelogger.services.database.HLDatabaseService
import org.itsadigitaltrust.hardwarelogger.services.{HLDatabaseService, HardwareGrabberService, HardwareIDValidationService}


trait TestServicesModule:
  given localhost: Boolean = true
  import org.itsadigitaltrust.hardwarelogger.backend.DataSourceLoader.given
  given databaseService: HLDatabaseService = MySQLHardwareLoggerDatabaseService()
  given hardwareGrabberService: HardwareGrabberService = OshiHardwareGrabberApplicationService
  given hardwareIDValidationService: HardwareIDValidationService = new SimpleHardwareIDValidationService
  

