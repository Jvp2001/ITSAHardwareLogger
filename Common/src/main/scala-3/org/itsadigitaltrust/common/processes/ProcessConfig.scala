package org.itsadigitaltrust.common.processes

trait ProcessConfig:
  lazy val sudoPassword: String
object ProcessConfig:
  given default: ProcessConfig with
    override lazy val sudoPassword: String = "itsa"