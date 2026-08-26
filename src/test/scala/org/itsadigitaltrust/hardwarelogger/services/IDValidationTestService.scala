package org.itsadigitaltrust.hardwarelogger.services

import org.itsadigitaltrust.hardwarelogger.services.HardwareIDValidationService.ValidationResult

class IDValidationTestService extends SimpleHardwareIDValidationService:
  override def validate(input: String): ValidationResult =
    val result =super.validate(input)
    println(result)
    result


