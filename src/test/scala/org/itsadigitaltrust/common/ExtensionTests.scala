package org.itsadigitaltrust.common

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.funsuite.AnyFunSuite

class ExtensionTests extends AnyFunSuite:
  test("Filter by Even Indices"):
    val number = "12345"
    assert(number.getEvenIndexItems == Seq('1', '3', '5'))

  test("Filter by Odd Indices"):
    val number = "12345"
    assert(number.getOddIndexItems == Seq('2', '4'))
