package org.itsadigitaltrust.hardwarelogger


import org.itsadigitaltrust.hardwarelogger.services.ServicesModule
import org.itsadigitaltrust.hardwarelogger.views.HardwareLoggerRootView


import oshi.SystemInfo
import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.Includes.{*, given}
import scalafx.scene.Scene
import scalafx.scene.layout.{BorderPane, Pane}

import scala.compiletime.uninitialized


object HardwareLoggerApplication extends JFXApp3:


  override def start(): Unit =

    stage = new PrimaryStage:
      minWidth = 600
      minHeight = 800
      title = "Hardware Logger"
      maximized = true
      scene = new Scene(1020, 720):
        root = new HardwareLoggerRootView
    stage.show()

end HardwareLoggerApplication


