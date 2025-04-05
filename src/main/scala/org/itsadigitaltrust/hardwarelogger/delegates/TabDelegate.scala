package org.itsadigitaltrust.hardwarelogger.delegates

import scalafx.scene.control.Tab

trait TabDelegate:
  def onSelected(tab: Tab): Unit = ()
