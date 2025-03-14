package org.itsadigitaltrust.hardwarelogger.core

import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.util.Callback
import org.springframework.boot.SpringBootConfiguration
import org.springframework.context.ConfigurableApplicationContext

import java.net.URL

class SFXMLLoader(private val springContext: ConfigurableApplicationContext):
  private val loader: FXMLLoader = new FXMLLoader()
  loader.setControllerFactory(springContext.getBean)
  
  def load[T](url: URL): T =
    loader.setLocation(url)
   
    loader.load()

  def load[T](klass: Class[?], path: String): T =
    load(klass.getResource(path).toURI.toURL)

