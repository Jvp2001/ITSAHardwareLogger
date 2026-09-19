package org.itsadigitaltrust.hardwarelogger.ui
import org.itsadigitaltrust.hardwarelogger.core.ThemeManager
import org.itsadigitaltrust.hardwarelogger.core.ui.*
import scalafx.scene.control.{CheckMenuItem, Menu, RadioMenuItem, ToggleGroup}
import scalafx.scene.input.{KeyCharacterCombination, KeyCodeCombination}
final class ThemeSelectionMenu extends Menu:
  private final class ThemeSelectionItem(mode: ThemeManager.ThemeMode) extends RadioMenuItem(s"_${mode.toString}"):
    onAction = _ => ThemeManager.themeMode = mode
  
  private val themeMenuItems =
    ThemeManager.ThemeMode.values.map(ThemeSelectionItem(_))

  private final val toggleGroup = ToggleGroup()
  toggleGroup.toggles = themeMenuItems

  themeMenuItems.foreach: item =>
    
    items.add(item)
  text = "_Theme"


