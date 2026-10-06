package org.itsadigitaltrust.hardwarelogger.contextbasedemnubar

import org.itsadigitaltrust.common.Operators.?
import scalafx.scene.control.TabPane
import scalafx.Includes.*

trait ContextBasedMenuBarTabPane(using menuManagerService: MenuManagerService)(using contextBasedMenuBar: ContextBasedMenuBar):
  this: TabPane =>
  