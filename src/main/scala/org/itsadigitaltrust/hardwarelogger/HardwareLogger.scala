package org.itsadigitaltrust.hardwarelogger

import javafx.application.Application

/** Seperated from the [[HardwareLoggerApplication]] due issues with Java 9+ modules. */

object HardwareLogger:
  def main(args: Array[String]): Unit =
    Application.launch(classOf[HardwareLoggerApplication], args*)
