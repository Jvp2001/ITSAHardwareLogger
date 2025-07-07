package org.itsadigitaltrust.hardwarelogger

import javafx.application.Preloader
import javafx.application.Preloader.StateChangeNotification
import javafx.stage.{Stage, StageStyle}
import scalafx.Includes.{*, given}
import scalafx.scene.Scene
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.StackPane
import scalafx.scene.paint.Color

import scala.compiletime.uninitialized

class HardwareLoggerSplashScreen extends Preloader:
  lazy final val stackPane = new StackPane()
  private var preloaderStage: Stage = uninitialized
  override def init(): Unit =
    val imageView = new ImageView:
      preserveRatio = true
      fitWidth = 500
      image = new Image(getClass.getResourceAsStream("assets/images/ITSA_Logo.jpeg"))
    stackPane.children += imageView
  end init

  override def start(stage: Stage): Unit =
    preloaderStage = stage
    val scene = new Scene(stackPane, 640, 480):
      fill = Color.Transparent
      
    stage.setScene(scene)
    stage.initStyle(StageStyle.TRANSPARENT)
    stage.centerOnScreen()
    stage.show()
  end start

  override def handleStateChangeNotification(stateChangeNotification: Preloader.StateChangeNotification): Unit =
    stateChangeNotification.getType match
      case StateChangeNotification.Type.BEFORE_LOAD => ()
      case StateChangeNotification.Type.BEFORE_INIT => ()
      case StateChangeNotification.Type.BEFORE_START => preloaderStage.close() 
  end handleStateChangeNotification
  
    
end HardwareLoggerSplashScreen


