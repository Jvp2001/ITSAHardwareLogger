package org.itsadigitaltrust.hardwarelogger


import com.mysql.cj.exceptions.CJCommunicationsException
import com.sun.javafx.PlatformUtil
import org.itsadigitaltrust.common
import common.Result
import core.ui.*
import org.itsadigitaltrust.hardwarelogger.HardwareLoggerApplication.getClass
import delegates.{ProgramMode, ProgramModeChangedDelegate}
import org.itsadigitaltrust.hardwarelogger.tasks.HLTaskRunner
import services.NotificationChannel.ProgramModeChanged
import services.{NotificationCentre, NotificationChannel, ServicesModule}
import views.HardwareLoggerRootView
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.application.{JFXApp3, Platform}
import scalafx.scene.Scene

import scala.compiletime.uninitialized


object HardwareLoggerApplication extends JFXApp3, ServicesModule, ProgramModeChangedDelegate:


  private val titleProperty: StringProperty = StringProperty("Hardware Logger")

  def setProgramMode(): Unit =
    ProgramMode.mode =
      if parameters.raw.map(_.toLowerCase).contains("--harddrive") then "HardDrive"
      else "Normal"

  end setProgramMode

  override def start(): Unit =
    setProgramMode()
    Platform.runLater:
      databaseService.connect(getClass, "db/db.properties")
    //          case Result.Success(_) => ()
    //          case Result.Error(err) => new Alert(AlertType.Error, "Could not connect to database!"):
    //            contentText = "Failed to connect to the database; please check your intranet connection, and try again!"
    //          .showAndWait()






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
      case e: java.net.ConnectException =>
        new Alert(AlertType.Error, "Failed to connect the database! Check internet connect!", ButtonType.OK).showAndWait()
  end start

  override def stopApp(): Unit =
    databaseService.stop()

  override def onProgramModeChanged(mode: ProgramMode): Unit =
    titleProperty.value = ProgramMode.mode match
      case "HardDrive" => "Hard Drive Logger"
      case "Normal" => "Hardware Logger"

end HardwareLoggerApplication


