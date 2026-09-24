package org.itsadigitaltrust.hardwarelogger.services.database

import org.itsadigitaltrust.common.Operators.??
import org.itsadigitaltrust.hardwarelogger.backend.HLDBErrorCode.NoEntriesRemoved
import org.itsadigitaltrust.hardwarelogger.backend.{ConnectionErrorType, HLDBErrorCode}
import org.itsadigitaltrust.hardwarelogger.services.HLFrontendException
import org.itsadigitaltrust.hardwarelogger.services.OshiHardwareGrabberApplicationService.HardwareGrabberException

sealed class HLDatabaseServiceExceptionBase(message: String) extends HLFrontendException(message)



final class HLDatabaseServiceException private(errorCode: HLDBErrorCode, message: String) extends HLDatabaseServiceExceptionBase(message)

final class HLDatabaseHardwareGrabberServiceException(message: String) extends HLDatabaseServiceExceptionBase(message)
object HLDatabaseHardwareGrabberServiceException:
  def apply(cause: HardwareGrabberException): HLDatabaseHardwareGrabberServiceException = new HLDatabaseHardwareGrabberServiceException(cause.getMessage)
object HLDatabaseServiceException:
  def apply(args: String*)(errorCode: HLDBErrorCode): HLDatabaseServiceException =
    apply(errorCode, args *)  
    
  def apply(errorCode: HLDBErrorCode, args: String*): HLDatabaseServiceException = errorCode match
    case HLDBErrorCode.EntryAlreadyExists => duplicateEntry(args(0), args(1))
    case HLDBErrorCode.EntryNotFound => recordNotFound(args(0), args(1))
    case HLDBErrorCode.NoEntriesFound => noEntriesFound(args(0))
    case HLDBErrorCode.PCSerialNumberAlreadyExists => duplicateEntry(args(0), args(1))
    case HLDBErrorCode.NoEntriesRemoved => noEntriesRemoved()
    case HLDBErrorCode.EntriesAlreadyExists => duplicateEntriesFound(args(0))
    case HLDBErrorCode.UnknownError(_) => unknownError()
    case HLDBErrorCode.ConnectionError(_, message) => connectionError(message = message)
    case HLDBErrorCode.PropertiesError => propertiesFileError()

  def noEntriesRemoved() = new HLDatabaseServiceException(HLDBErrorCode.NoEntriesRemoved, "No entries removed!")
  def noEntriesFound(id: String) = new HLDatabaseServiceException(HLDBErrorCode.NoEntriesFound, s"No entries found for id: $id")

  def noEntryFound(model: String, id: String): HLDatabaseServiceException = new HLDatabaseServiceException(HLDBErrorCode.EntryNotFound, s"No entry found for model: $model and serial: $id")

  def duplicateEntriesFound(model: String): HLDatabaseServiceException = new HLDatabaseServiceException(HLDBErrorCode.EntryAlreadyExists, s"Duplicate entries found for model: $model")


  def duplicateEntry(model: String, serial: String): HLDatabaseServiceException = new HLDatabaseServiceException(HLDBErrorCode.EntryAlreadyExists, s"Duplicate entry found for model: $model and serial: $serial")

  def recordNotFound(model: String, serial: String): HLDatabaseServiceException = new HLDatabaseServiceException(HLDBErrorCode.EntryNotFound, s"Record not found for model: $model and serial: $serial")

  def connectionError(`type`: ConnectionErrorType = "Unknown", message: String = "Failed to connect to the database!"): HLDatabaseServiceException = new HLDatabaseServiceException(HLDBErrorCode.ConnectionError(`type`), message)

  def unknownError(message: String = "Unknown Error!"): HLDatabaseServiceException = new HLDatabaseServiceException(HLDBErrorCode.UnknownError(), message)

  def propertiesFileError(message: String = "Failed to load propertiesFilePath file!"): HLDatabaseServiceException = new HLDatabaseServiceException(HLDBErrorCode.PropertiesError, message)
end HLDatabaseServiceException