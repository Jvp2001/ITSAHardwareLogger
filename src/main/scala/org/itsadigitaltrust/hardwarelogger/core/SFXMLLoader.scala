package org.itsadigitaltrust.hardwarelogger.core

import javafx.fxml.FXMLLoader
import javafx.scene.Parent

import java.net.URL

object SFXMLLoader:
  def load[T](url: URL): T =
    val loader = new FXMLLoader(url)
    loader.load()

  def load[T](path: String): T =
    load(getClass.getResource(path).toURI.toURL)

