package org.itsadigitaltrust.hardwarelogger


import com.sun.javafx.PlatformUtil
import org.itsadigitaltrust.common
import org.itsadigitaltrust.common.{Result, Success}
import org.itsadigitaltrust.hardwarelogger.HardwareLoggerApplication.getClass
import org.itsadigitaltrust.hardwarelogger.services.ServicesModule
import org.itsadigitaltrust.hardwarelogger.views.HardwareLoggerRootView
import oshi.SystemInfo
import scalafx.application.{JFXApp3, Platform}
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.Includes.{*, given}
import scalafx.scene.Scene
import scalafx.scene.control.{Alert, ButtonType}
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.layout.{BorderPane, Pane}

import scala.compiletime.uninitialized


object HardwareLoggerApplication extends JFXApp3, ServicesModule:


  override def start(): Unit =
    Platform.runLater:
      databaseService.connect(getClass, "db/db.properties") match
        case Success(_) => ()
        case common.Error(reason) =>
          new Alert(AlertType.Error, reason, ButtonType.OK).showAndWait()

    stage = new PrimaryStage:
      minWidth = 600
      minHeight = 800
      title = "Hardware Logger"
      maximized = true
      scene = new Scene(1020, 720):
        root = new HardwareLoggerRootView
      show()

  end start

  override def stopApp(): Unit =
    databaseService.stop()
end HardwareLoggerApplication


