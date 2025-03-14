package org.itsadigitaltrust.hardwarelogger

import javafx.application.{Application, Platform}
import javafx.scene.control.{TableColumn, TableView}
import javafx.scene.{Parent, Scene}
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import org.itsadigitaltrust.hardwarelogger.core.SFXMLLoader
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.ComponentScan
import oshi.SystemInfo

import scala.compiletime.uninitialized


@ComponentScan(basePackages = Array("org.itsadigitaltrust.hardwarelogger"))
@SpringBootApplication
class HardwareLoggerApplication extends Application:
  private var springContext: ConfigurableApplicationContext = uninitialized
  private var sfmlLoader: SFXMLLoader = uninitialized
  private var rootNode: Parent = uninitialized

  override def init(): Unit =
    springContext = SpringApplication.run(classOf[HardwareLoggerApplication])
     sfmlLoader = new SFXMLLoader(springContext)
    rootNode = sfmlLoader.load(getClass, "HardwareLoggerRootView.fxml")

  override def start(stage: Stage): Unit =
    stage.setMinWidth(600)
    stage.setMinHeight(800)
    stage.setTitle("Hardware Logger")
//    Platform.runLater: () =>
//    Platform.runLater: () =>
    stage.setMaximized(true)
    stage.setScene(new Scene(rootNode, 1020, 720))
    stage.show()


  override def stop(): Unit =
    springContext.close()

end HardwareLoggerApplication


