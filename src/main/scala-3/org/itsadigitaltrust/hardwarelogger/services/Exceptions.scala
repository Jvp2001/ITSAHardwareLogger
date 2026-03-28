package org.itsadigitaltrust.hardwarelogger.services

abstract class HLFrontendException(message: String) extends RuntimeException(message)

final class InvalidITSAIDException(message: String) extends HLFrontendException(message)
final class InvalidSerialNumberException(message: String) extends HLFrontendException(message)