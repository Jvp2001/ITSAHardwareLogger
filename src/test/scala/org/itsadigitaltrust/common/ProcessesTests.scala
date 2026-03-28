package org.itsadigitaltrust.common

import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.common.processes.{Lsblk, ProcessConfig}

import org.scalatest.funsuite.AnyFunSuite

final class ProcessesTests extends AnyFunSuite with HWLLoggable:
  given processConfig: ProcessConfig = new ProcessConfig:
    override lazy val sudoPassword: String = "password"

  test("Lsblk Parses successfully"):
    assert(Lsblk()("/dev/sda") != Option(Map.empty))

