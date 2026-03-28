package org.itsadigitaltrust.hardwarelogger


import com.mysql.cj.exceptions.CJCommunicationsException
import com.sun.javafx.PlatformUtil
import org.itsadigitaltrust.common
import common.Result

import org.itsadigitaltrust.hardwarelogger.core.ui.*

import scalafx.Includes.*
import delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.core.HardwareLoggerDefaultUncaughtExceptionHandler
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{NotificationCentreService, NotificationName, ReloadMessageArgs}
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.NotificationName.ProgramModeChanged
import services.ApplicationServicesRegistry
import views.HardwareLoggerRootView

import scalafx.application.JFXApp3.PrimaryStage
import scalafx.application.{JFXApp3, Platform}
import scalafx.scene.Scene

import scala.compiletime.uninitialized


object HardwareLoggerApplication extends JFXApp3, ApplicationServicesRegistry:


  private val titleProperty: StringProperty = StringProperty("Hardware Logger")

  //System.setProperty("javafx.preloader", classOf[HardwareLoggerSplashScreen].getName)

  def setProgramMode(): Unit =
    ProgramMode.mode =
      if parameters.raw.map(_.toLowerCase).contains("--harddrive") then "HardDrive"
      else "Normal"

  end setProgramMode

  private def reload(): Unit =
    notificationCentre.post[ReloadMessageArgs](NotificationName.Reload, ReloadMessageArgs())


  override def start(): Unit =
    setProgramMode()
    Thread.setDefaultUncaughtExceptionHandler(HardwareLoggerDefaultUncaughtExceptionHandler())

    stage = new PrimaryStage:
      minWidth = 1024
      minHeight = 768
//
      scene = new Scene:
        root = new HardwareLoggerRootView
        onKeyPressed = (event: KeyEvent) =>
          val code = event.code
          if code == KeyCode.F5 then
           reload()
      show()
  end start

  override def stopApp(): Unit =
    databaseService.stop()


  def onProgramModeChanged(mode: ProgramMode): Unit =
    titleProperty.value = ProgramMode.mode match
      case "HardDrive" => "Hard Drive Logger"
      case "Normal" => "Hardware Logger"

end HardwareLoggerApplication


