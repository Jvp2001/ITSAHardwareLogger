package org.itsadigitaltrust.hardwarelogger.core

import scalafx.application.{ColorScheme, Platform}
import scalafx.beans.property.{BooleanProperty, ObjectProperty, ReadOnlyBooleanProperty}
import scalafx.application.JFXApp3.Parameters

object ThemeManager:
  enum ThemeMode(private final val label: String) extends java.lang.Enum[ThemeMode]:
    case System extends ThemeMode("Follow System")
    case Light extends ThemeMode("Light Mode")
    case Dark extends ThemeMode("Dark Mode")

    override def toString: String = label

  import ThemeMode.*

  final val themeModeProperty: ObjectProperty[ThemeMode] = new ObjectProperty(this, "themeMode", ThemeMode.System)
  private final val darkThemeActive = new BooleanProperty(this, "darkThemeActive", false)

  /**
   * Helper function to evaluate the active theme state.
   * Compares the platform independent strong property type wrappers directly.
   */
  private def calculateIsDark(): Boolean =
    val mode = themeModeProperty()
    if mode == ThemeMode.System then
      // Bypasses Map keys entirely; checks ScalaFX and JavaFX native types cleanly
      val scheme = Platform.preferences.colorScheme.value
      scheme == ColorScheme.Dark || scheme.toString == "DARK"
    else
      mode == Dark

  /**
   * Configures live auto-theme tracking hooks on the FX Application Thread.
   */
  def init(): Unit =
    // Set the initial theme state when the application launches
    darkThemeActive.value = calculateIsDark()

    // Listen for manual settings changes (e.g., from an in-app dropdown)
    themeModeProperty.addListener: (obs, oldMode, newMode) =>
      darkThemeActive.value = calculateIsDark()

    // FIX: Listen to the strongly-typed ColorScheme property wrapper rather than the Map layout.
    // This allows JavaFX to broadcast the native DBus window preferences cleanly on Linux.
    Platform.preferences.colorSchemeProperty().addListener: (obs, oldScheme, newScheme) =>
      scalafx.application.Platform.runLater: () =>
        darkThemeActive.value = calculateIsDark()

  def parseArgs(parameters: Parameters): Unit =
    val namedParams = parameters.named
    if namedParams.contains("theme") then
      namedParams("theme").toLowerCase.trim match
        case "dark"  => themeMode = Dark
        case "light" => themeMode = Light
        case _       => themeMode = System

  def themeMode: ThemeMode = themeModeProperty()
  def themeMode_=(newMode: ThemeMode): Unit = themeModeProperty() = newMode

  def darkThemeActiveProperty: ReadOnlyBooleanProperty = darkThemeActive
  def isDarkThemeActive: Boolean = darkThemeActive()

end ThemeManager
