package org.itsadigitaltrust.hardwarelogger


import com.sun.javafx.PlatformUtil
import org.itsadigitaltrust.common
import common.{Error, Result, Success}
import core.ui.*
import org.itsadigitaltrust.hardwarelogger.HardwareLoggerApplication.getClass
import delegates.{ProgramMode, ProgramModeChangedDelegate}
import services.NotificationChannel.ProgramModeChanged
import services.{NotificationCentre, NotificationChannel, ServicesModule}
import views.HardwareLoggerRootView
import oshi.SystemInfo
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.application.{JFXApp3, Platform}
import scalafx.scene.Scene
import scalafx.scene.input.{KeyCode, KeyEvent}
import scalafx.Includes.*

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
        onKeyPressed = (event: KeyEvent) =>
          val code = event.code
          if code == KeyCode.F5 then
            notificationCentre.publish(NotificationChannel.Reload)


      show()

  end start

  override def stopApp(): Unit =
    databaseService.stop()
end HardwareLoggerApplication


