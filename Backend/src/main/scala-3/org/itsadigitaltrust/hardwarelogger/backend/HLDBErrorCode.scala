package org.itsadigitaltrust.hardwarelogger.backend

type ConnectionErrorType = "Username" | "Password" | "Address" | "Unknown"

enum HLDBErrorCode:
  case EntryNotFound extends HLDBErrorCode
  case NoEntriesFound extends HLDBErrorCode
  case EntryAlreadyExists extends HLDBErrorCode
  case EntriesAlreadyExists extends HLDBErrorCode
  case PCSerialNumberAlreadyExists extends HLDBErrorCode
  case UnknownError extends HLDBErrorCode
  case ConnectionError(`type`: ConnectionErrorType = "Unknown") extends HLDBErrorCode
  case PropertiesError extends HLDBErrorCode
end HLDBErrorCode

type HLDBTransactionResult[T] = Either[HLDBErrorCode, T]
