package org.itsadigitaltrust.hardwarelogger.core

import javafx.beans.property.SimpleBooleanProperty
import org.itsadigitaltrust.common.Operators.{in, |>}
import org.itsadigitaltrust.common.in
import scalafx.application.{ColorScheme, Platform}
import scalafx.beans.Observable
import scalafx.beans.binding.{Bindings, BooleanBinding}
import scalafx.beans.property.{BooleanProperty, ObjectProperty, ReadOnlyBooleanProperty}
import scalafx.Includes.*
import scalafx.application.JFXApp3.Parameters
extension (bindings: Bindings)
  def createBooleanBinding(dependencies: Observable*)(func: () => Boolean): BooleanBinding =
    Bindings.createBooleanBinding(func, dependencies*)

object ThemeManager:
  enum ThemeMode(private final val label: String) extends java.lang.Enum[ThemeMode]:
    case System extends ThemeMode("Follow System")
    case Light extends ThemeMode("Light Mode")
    case Dark extends ThemeMode("Dark Mode")

    override def toString: String = label

  end ThemeMode

  import ThemeMode.*
  final val themeModeProperty: ObjectProperty[ThemeMode] = new ObjectProperty()

  private final val darkThemeActive = new BooleanProperty()

  darkThemeActive.bind(Bindings.createBooleanBinding(themeModeProperty, Platform.preferences.colorSchemeProperty()): () =>
    val mode = themeModeProperty()
    if mode == ThemeMode.System then
      Platform.preferences.colorScheme == ColorScheme.Dark
    else
      mode == Dark
  )
  
  
  def parseArgs(parameters: Parameters): Unit =
    val namedParams = parameters.named
    if namedParams.contains("theme") then
      namedParams("theme").toLowerCase.trim match
        case "dark" => themeMode = Dark
        case "light" => themeMode = Light
        case _ => themeMode = System
        
        
  def themeMode: ThemeMode = themeModeProperty()
  def themeMode_=(newMode: ThemeMode): Unit = themeModeProperty() = newMode
  
  def darkThemeActiveProperty: ReadOnlyBooleanProperty =
    darkThemeActive
    
  def isDarkThemeActive: Boolean =
    darkThemeActive()
      
    
end ThemeManager

