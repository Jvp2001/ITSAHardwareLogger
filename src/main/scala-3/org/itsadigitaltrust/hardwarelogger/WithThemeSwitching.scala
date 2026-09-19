package org.itsadigitaltrust.hardwarelogger

import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.hardwarelogger.core.ThemeManager
import scalafx.application.Platform
import scalafx.scene.Scene
import scalafx.collections.ObservableBuffer
import atlantafx.base.theme.Theme // Import AtlantaFX structural Theme type

trait WithThemeSwitching extends HWLLoggable:
  // Provide AtlantaFX theme definitions instead of raw text file paths
  protected def darkAtlantaTheme: Theme
  protected def lightAtlantaTheme: Theme

  private val activeScenes = ObservableBuffer[Scene]()

  protected def registerScene(scene: Scene): Unit =
    if !activeScenes.contains(scene) then
      activeScenes.add(scene)
      // On scene addition, guarantee the current layout is aligned
      applyThemeState(ThemeManager.isDarkThemeActive)

  protected def setupThemeManager(): Unit =
    ThemeManager.init()

    ThemeManager.darkThemeActiveProperty.addListener: (obs, wasDark, isDark) =>
      Platform.runLater(() => applyThemeState(isDark))

    // Initialize styling profiles cleanly on startup
    applyThemeState(ThemeManager.isDarkThemeActive)

  private def applyThemeState(isDark: Boolean): Unit =
    val targetTheme = if isDark then darkAtlantaTheme else lightAtlantaTheme
    logger.info(s"Switching AtlantaFX environment to: ${targetTheme.getName}")

    // CRITICAL: Tell AtlantaFX to inject its internal user-agent variables globally.
    // This dynamically updates TableViews across all active scenes simultaneously!
    javafx.application.Application.setUserAgentStylesheet(targetTheme.getUserAgentStylesheet)

end WithThemeSwitching
