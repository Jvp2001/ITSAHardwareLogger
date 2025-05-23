package org.itsadigitaltrust.hardwarelogger.core

import org.itsadigitaltrust.common.{PropertyFileReader, Result}
import org.itsadigitaltrust.common.processes.ProcessConfig


given HLProcessConfig: ProcessConfig = new ProcessConfig:
  override lazy val sudoPassword: String = "password"
//        PropertyFileReader("org/itsadigitaltrust/hardwarelogger/config.properties")("shell.sudo.password") match
//          case Result.Result.Result.Success(value) => value
//          case _ => ""
