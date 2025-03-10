package org.itsadigitaltrust.hardwarelogger

import javafx.application.{Application, Platform}
import javafx.scene.control.{TableColumn, TableView}
import javafx.scene.{Parent, Scene}
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import org.itsadigitaltrust.hardwarelogger.core.SFXMLLoader
import oshi.SystemInfo


class HardwareLoggerApplication extends Application:

  override def start(stage: Stage): Unit =
    stage.setMinWidth(600)
    stage.setMinHeight(800)
    stage.setTitle("Hardware Logger")
//    Platform.runLater: () =>
//    Platform.runLater: () =>
    stage.setMaximized(true)
    val si = new SystemInfo()
    val hal = si.getHardware()
    val cpu = hal.getProcessor()
    println(hal.getDiskStores)
    val value: Parent = SFXMLLoader.load(getClass.getResource("HardwareLoggerDemoView.fxml"))
    stage.setScene(new Scene(value, 1020, 720))
    stage.show()



