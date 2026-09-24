package org.itsadigitaltrust.hardwarelogger.backend


type ConnectionErrorType = "Username" | "Password" | "Address" | "Unknown"

enum HLDBErrorCode:
  case EntryNotFound extends HLDBErrorCode
  case NoEntriesFound extends HLDBErrorCode
  case NoEntriesRemoved extends HLDBErrorCode
  case EntryAlreadyExists extends HLDBErrorCode
  case EntriesAlreadyExists extends HLDBErrorCode
  case PCSerialNumberAlreadyExists extends HLDBErrorCode
  case UnknownError(message: String = "") extends HLDBErrorCode
  case ConnectionError(`type`: ConnectionErrorType = "Unknown", message: String = "") extends HLDBErrorCode
  case PropertiesError extends HLDBErrorCode
end HLDBErrorCode

private type HLDBTransactionLabel[T] = scala.util.boundary.Label[HLDBTransactionResult[T]]
type HLDBTransactionResult[T] = Either[HLDBErrorCode, T]
