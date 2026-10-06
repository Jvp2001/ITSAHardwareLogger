package org.itsadigitaltrust.hardwarelogger.contextbasedemnubar

import javafx.beans.property.SimpleBooleanProperty
import scalafx.beans.property.ReadOnlyBooleanProperty

/**
 * Pure data representation of a desktop keyboard accelerator combo.
 *
 * @param modifiers The structural keys (e.g., "Ctrl", "Shift", "Alt")
 * @param key      The primary input character code (e.g., "I", "S", "Delete")
 */
case class KeyShortcut(key: String, modifiers: ("Ctrl" | "Shift" | "Alt" | "Meta" | "Shortcut")* )

/**
 * The base Menu data type
 */
sealed trait MenuElement:
  def menuPath: String

/**
 * Pure data layout descriptor representing a target menu action.
 * Holds no references to graphical components.
 *
 * @param menuPath          The target path layout branch structure (e.g., "Tools / Options")
 * @param itemLabel         The visual textual string displayed on screen (e.g., "More Info (i)")
 * @param actionId          The decoupled string payload ID matching command dispatcher patterns
 * @param shortcut          Optional keyboard accelerator bindings
 */
final case class MenuActionDefinition(
                                 menuPath: String,
                                 itemLabel: String,
                                 actionId: String,
                                 shortcut: Option[KeyShortcut] = None
                               ) extends MenuElement

final case class MenuSeparatorDefinition(menuPath: String) extends MenuElement

/**
 * Pure MVVM context provider contract. ViewModels implement this to announce
 * custom application actions without leaking the presentation layer skin.
 */
trait MenuContributor:

  /**
   * Returns a collection of pure data layout records describing
   * custom menu configuration fields to map at runtime.
   */
  def getContributedActions: List[MenuElement] = Nil

  /**
   * The structural entry point handler where raw user click events
   * are routed straight back into the business logic.
   *
   * @param actionId The target identifier tracking what action string fired
   */
  def handleMenuAction(actionId: String): Unit = ()

  /**
   * Returns a reactive property tracking if a specific action item should be enabled.
   * The custom MenuBar dynamically binds to this over time.
   *
   * @param actionId The identifier string matching target menu options
   */
  def getActionEnabledProperty(actionId: String): ReadOnlyBooleanProperty =
    ReadOnlyBooleanProperty(this, "defaultEnabled", true)

end MenuContributor
