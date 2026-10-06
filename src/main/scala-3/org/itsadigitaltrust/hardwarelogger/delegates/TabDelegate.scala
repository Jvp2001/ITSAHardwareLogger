package org.itsadigitaltrust.hardwarelogger.delegates

import org.itsadigitaltrust.common.Operators.?
import org.itsadigitaltrust.hardwarelogger.contextbasedemnubar.{MenuContributor, MenuManagerService}
import scalafx.scene.control.Tab

trait TabDelegate(using menuManagerService: MenuManagerService):
  def onSelected(tab: Tab): Unit