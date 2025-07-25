package org.itsadigitaltrust.hardwarelogger.tasks

import org.scalafx.extras.BusyWorker
import org.itsadigitaltrust.hardwarelogger.core.ui.*
import scalafx.Includes.*
import javafx.scene as jfxs

class HLBusyWorker(
                     title: String,
                     window: Option[Window] = None
                   ) extends BusyWorker(title, window):
  override def showException(title: String, message: String, t: Throwable, resizable: Boolean): Unit =
    ()


