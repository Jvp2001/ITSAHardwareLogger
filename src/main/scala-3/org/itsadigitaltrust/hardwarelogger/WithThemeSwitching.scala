package org.itsadigitaltrust.hardwarelogger

import javafx.application.Application
import javafx.collections as jfxc
import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.hardwarelogger.core.ThemeManager
import scalafx.application.JFXApp3
import scalafx.beans.property.ObjectProperty
import scalafx.collections.ObservableBuffer
import scalafx.scene.Scene

trait WithThemeSwitching extends HWLLoggable:
  this: Scene =>
  protected lazy val darkModeStylesheet: String
  protected lazy val lightModeStylesheet: String
  protected val useUserAgentStylesheet: Boolean = false
  
  ThemeManager.darkThemeActiveProperty.addListener: (obs, wasDark, isDark) =>
    applyStylesheet(isDark)

  protected def applyStylesheet(isDark: Boolean = ThemeManager.isDarkThemeActive): Unit =
    updateUserAgentStylesheet(isDark)

  end applyStylesheet
  
  private def updateUserAgentStylesheet(isDark: Boolean): Unit =
    if isDark then
      Application.setUserAgentStylesheet(darkModeStylesheet)
    else
      Application.setUserAgentStylesheet(lightModeStylesheet)

  applyStylesheet()
end WithThemeSwitching


