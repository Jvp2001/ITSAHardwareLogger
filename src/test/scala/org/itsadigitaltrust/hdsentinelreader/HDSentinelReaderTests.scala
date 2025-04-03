package org.itsadigitaltrust.hdsentinelreader

import org.scalatest.funsuite.AnyFunSuite

class HDSentinelReaderTests extends AnyFunSuite:
  test("Get XML output"):
    val value = HDSentinelReader("password", XMLFile.from(getClass.getResource("hdsentinel.xml").getPath))
    println(value)
    assert(value != null)
