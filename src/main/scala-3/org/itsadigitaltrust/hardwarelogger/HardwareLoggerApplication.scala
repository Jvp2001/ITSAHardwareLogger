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
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.application.{JFXApp3, Platform}
import scalafx.scene.Scene

import scala.compiletime.uninitialized


object HardwareLoggerApplication extends JFXApp3, ServicesModule:


  private val titleProperty: StringProperty = StringProperty("Hardware Logger")

  def setProgramMode(): Unit =
    ProgramMode.mode =
      if parameters.raw.map(_.toLowerCase).contains("--harddrive") then "HardDrive"
      else "Normal"
    titleProperty.value = ProgramMode.mode match

      case "HardDrive" => "Hard Drive"
      case "Normal" => "Hardware Logger"

  end setProgramMode

  override def start(): Unit =
    setProgramMode()
    Platform.runLater:
      databaseService.connect(getClass, "db/db.properties") match
        case Success(_) => ()
        case Error(reason) =>
          new Alert(AlertType.Error, reason, ButtonType.OK).showAndWait()

    try

      stage = new PrimaryStage:
        minWidth = 600
        minHeight = 800
        maximized = true
        title <==> titleProperty
        scene = new Scene(1020, 720):
          root = new HardwareLoggerRootView
//            menuBar.useSystemMenuBar =  true
          onKeyPressed = (event: KeyEvent) =>
            val code = event.code
            if code == KeyCode.F5 then
              hardwareGrabberService.load(): () =>
                notificationCentre.publish(NotificationChannel.Reload)



        show()
    catch
      case e: NumberFormatException =>
        e.printStackTrace()
  end start

  override def stopApp(): Unit =
    databaseService.stop()
end HardwareLoggerApplication


