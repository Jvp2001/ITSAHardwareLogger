package org.itsadigitaltrust.hardwarelogger.traits

import org.itsadigitaltrust.hardwarelogger.services.TestDependencies
import org.scalatest.EitherValues
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers => ShouldMatchers

trait HardwareLoggerTestSuit extends TestDependencies with ShouldMatchers with AnyFunSuiteLike with EitherValues