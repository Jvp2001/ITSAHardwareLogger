package org.itsadigitaltrust.services.validation

import org.itsadigitaltrust.hardwarelogger.services.SimpleHardwareIDValidationService
import org.scalatest.funsuite.AnyFunSuite

class IDValidationTests extends AnyFunSuite:
  val validator = new SimpleHardwareIDValidationService()
  test("Valid Five Digit Id"):
    val id = "92807.0"
    val result = validator.validate(id)

    assert(result == Right(true))

  test("Valid Four Digit Id"):
    val id = "2476.9"
    val result = validator.validate(id)

    assert(result == Right(true))

