package org.itsadigitaltrust.hardwarelogger


import com.sun.javafx.PlatformUtil
import org.itsadigitaltrust.common
import org.itsadigitaltrust.common.{Error, Result, Success}
import org.itsadigitaltrust.hardwarelogger.HardwareLoggerApplication.getClass
import org.itsadigitaltrust.hardwarelogger.delegates.{ProgramMode, ProgramModeChangedDelegate}
import org.itsadigitaltrust.hardwarelogger.services.NotificationChannel.ProgramModeChanged
import org.itsadigitaltrust.hardwarelogger.services.{NotificationCentre, NotificationChannel, ServicesModule}
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


object HardwareLoggerApplication extends JFXApp3, ServicesModule, ProgramModeChangedDelegate:


  def setProgramMode(): Unit =
    ProgramMode.mode =
      if parameters.raw.map(_.toLowerCase).contains("--harddrive") then "HardDrive"
      else "Normal"

  end setProgramMode


  override def onProgramModeChanged(mode: ProgramMode): Unit =
    Platform.runLater:
      stage.title.value = if mode == "HardDrive" then "Hard Drive Logger" else "Hardware Logger"



  override def start(): Unit =
    setProgramMode()
    Platform.runLater:
      databaseService.connect(getClass, "db/db.properties") match
        case Success(_) => ()
        case Error(reason) =>
          new Alert(AlertType.Error, reason, ButtonType.OK).showAndWait()

    stage = new PrimaryStage:
      minWidth = 600
      minHeight = 800
      maximized = true
      scene = new Scene(1020, 720):
        root = new HardwareLoggerRootView
      show()

  end start

  override def stopApp(): Unit =
    databaseService.stop()
end HardwareLoggerApplication


