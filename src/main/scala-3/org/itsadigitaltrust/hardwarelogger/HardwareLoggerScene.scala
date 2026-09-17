package org.itsadigitaltrust.hardwarelogger
import atlantafx.base.theme.{PrimerDark, PrimerLight}
import core.ui.*
import scalafx.scene.{Parent, Scene}

final class HardwareLoggerScene(parent: Parent, width: Double = -1, height: Double = -1) extends Scene(parent, width, height), WithThemeSwitching:
  override protected lazy val darkModeStylesheet: String = new PrimerDark().getUserAgentStylesheet
  override protected lazy val lightModeStylesheet: String = new PrimerLight().getUserAgentStylesheet
  
end HardwareLoggerScene
