package org.itsadigitaltrust.hdsentinelreader

import org.scalatest.funsuite.AnyFunSuite

class HDSentinelReaderTests extends AnyFunSuite:
  test("Get XML output"):
    val value = HDSentinelReader("password", XMLFile("report.xml"))
    println(value)
    assert(value != null)
