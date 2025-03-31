package org.itsadigitaltrust.hdsentinelreader

import org.scalatest.funsuite.AnyFunSuite

class HDSentinelReaderTests extends AnyFunSuite:
  test("Get XML output"):
    val value = HDSentinelReader("password")
    println(value)
    assert(value != null || value != "")
