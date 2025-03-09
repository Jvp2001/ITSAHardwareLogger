package org.itsadigitaltrust.hardwarelogger

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import org.itsadigitaltrust.hardwarelogger.core.SFXMLLoader


class HardwareLoggerApplication extends Application:

  override def start(stage: Stage): Unit =
    stage.setMinWidth(600)
    stage.setMinHeight(800)
    stage.setTitle("Hardware Logger")
    stage.setScene(new Scene(SFXMLLoader.load(getClass.getResource("HardwareLoggerRootView.fxml")), 1020, 720))
    stage.show()


