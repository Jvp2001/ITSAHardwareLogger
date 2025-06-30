package org.itsadigitaltrust.hardwarelogger


import com.mysql.cj.exceptions.CJCommunicationsException
import com.sun.javafx.PlatformUtil
import org.itsadigitaltrust.common
import common.Result
import core.ui.*
import org.itsadigitaltrust.hardwarelogger.HardwareLoggerApplication.getClass
import delegates.{ProgramMode, ProgramModeChangedDelegate}
import org.itsadigitaltrust.hardwarelogger.core.HardwareLoggerDefaultUncaughtExceptionHandler
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{NotificationCentre, NotificationName}
import org.itsadigitaltrust.hardwarelogger.tasks.HLTaskRunner
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.NotificationName.ProgramModeChanged
import services.ServicesModule
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
    Thread.setDefaultUncaughtExceptionHandler(HardwareLoggerDefaultUncaughtExceptionHandler())

    stage = new PrimaryStage:
      minWidth = 1024
      minHeight = 768
      maximized = true
      title <==> titleProperty
      scene = new Scene(1020, 720):
        root = new HardwareLoggerRootView
        //            menuBar.useSystemMenuBar =  true
//        onKeyPressed = (event: KeyEvent) =>
//          val code = event.code
//          if code == KeyCode.F5 then
//            notificationCentre.post(NotificationName.Reload)
      show()

  end start

  override def stopApp(): Unit =
    databaseService.stop()

    notificationCentre.close()

  override def onProgramModeChanged(mode: ProgramMode): Unit =
    titleProperty.value = ProgramMode.mode match
      case "HardDrive" => "Hard Drive Logger"
      case "Normal" => "Hardware Logger"

end HardwareLoggerApplication


