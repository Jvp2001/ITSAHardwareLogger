package org.itsadigitaltrust.hardwarelogger


import org.itsadigitaltrust.common.Operators.|>
import org.itsadigitaltrust.hardwarelogger.core.ui.*
import org.itsadigitaltrust.hardwarelogger.core.{HWLoggerShowMessage, HardwareLoggerDefaultUncaughtExceptionHandler}
import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.services.ApplicationServicesRegistry
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState
import org.itsadigitaltrust.hardwarelogger.views.HardwareLoggerRootView
import scalafx.Includes.*
import scalafx.application.{JFXApp3, Platform}
import scalafx.scene.Scene
import scalafx.stage.{Stage, StageStyle}

import scala.compiletime.uninitialized


given useLocalhost: Boolean = true
object HardwareLoggerApplication extends JFXApp3, ApplicationServicesRegistry, HWLoggerShowMessage:

  Thread.setDefaultUncaughtExceptionHandler(HardwareLoggerDefaultUncaughtExceptionHandler())

  private given applicationState: HardwareLoggerApplicationState = new HardwareLoggerApplicationState

  private lazy val titleProperty: StringProperty = StringProperty("Hardware Logger")

  private var applicationStage: Stage | Null = uninitialized




  Platform.implicitExit = false




  private def setProgramMode(): Unit =
    ProgramMode.mode =
      if parameters.raw.map(_.toLowerCase).contains("--harddrive") then "HardDrive"
      else "Normal"

  end setProgramMode

  private final val applicationSize = (1024D, 768D)

  private val hardwareLoggerSplashScreenViewModel: HardwareLoggerSplashScreenViewModel = new HardwareLoggerSplashScreenViewModel()(applicationState =>
    Platform.runLater:
      showApplication()
  )

  override def start(): Unit =
    setProgramMode()
    val splashScreen = new HardwareLoggerSplashScreen(480, 360,hardwareLoggerSplashScreenViewModel)(showApplication)
    val splashStage =  new JFXApp3.PrimaryStage:
      initStyle(StageStyle.Undecorated)
      alwaysOnTop = true
      maxWidth = splashScreen.maxWidth()
      maxHeight = splashScreen.maxHeight()
//      initStyle(StageStyle.Undecorated)
      scene = new Scene:
        root = splashScreen // extends VBox


    stage = splashStage
    stage.onShown = _ =>
      splashScreen.load()
    stage.show()

  end start




  private def createApplicationStage()(using applicationState: HardwareLoggerApplicationState): Stage =
    val mainStage = new Stage:
      private val thisStage = this

      minWidth = applicationSize._1
      minHeight = applicationSize._2
      resizable = false
      
      scene = new Scene(applicationSize._1, applicationSize._2):
        root =   new HardwareLoggerRootView:
          onKeyPressed = (event: KeyEvent) =>
            val code = event.code
            if code == KeyCode.F5 then
              viewModel.reload()
            end if

//
      onCloseRequest = event =>
        Platform.exit()

    end mainStage
//    mainStage.scene().root.asInstanceOf[HardwareLoggerRootView].viewModel.parentWindow = Some(mainStage)


    mainStage

  end createApplicationStage

  private def showApplication()(using applicationState: HardwareLoggerApplicationState): Unit =
     applicationStage = createApplicationStage()
//
//    val applicationTransition = new FadeTransition(Duration(1500), applicationStage.getScene.getRoot):
//      fromValue = 0D
//      toValue = 1D
//      onFinished = () =>
//        applicationStage.show()
//
//    val splashScreenTransition = new FadeTransition(Duration(1500), stage.getScene.getRoot):
//      fromValue = 1D
//      toValue = 0D
//      delay = Duration(500)
//      onFinished = () =>
//        stage.hide()
//        applicationTransition.play()
//    end splashScreenTransition
//
//    splashScreenTransition.play()


      stage.hide()
      applicationStage.show()


  end showApplication



  override def stopApp(): Unit =
    databaseService.stop()


  def onProgramModeChanged(mode: ProgramMode): Unit =
    titleProperty.value = ProgramMode.mode.runtimeChecked match
      case "HardDrive" => "Hard Drive Logger"
      case "Normal" => "Hardware Logger"

  override protected def parentWindow: Option[Window] = Option(stage)
end HardwareLoggerApplication


