package org.itsadigitaltrust.hardwarelogger.contextbasedemnubar

import javafx.scene.control.SeparatorMenuItem
import scalafx.scene.control.{Menu, MenuBar, MenuItem}
import scalafx.Includes.*
import scalafx.event.ActionEvent
import scalafx.scene.input.KeyCombination

import scala.annotation.tailrec

class ContextBasedMenuBar(val baseMenus: Menu*)(using menuManagerService: MenuManagerService) extends MenuBar:

  baseMenus.foreach(_.mnemonicParsing = true)

  private final val initialItemCounts: Map[Menu, Int] =
    baseMenus.map(m => m -> m.items.size).toMap

  menus = baseMenus.toList


  menuManagerService.activeContributor.onChange: (_, _, newValue) =>
    newValue match
      case Some(contributor) => updateMenu(Some(contributor))
      case None => updateMenu(None) // Forces total cleanup of old tab items

  // =========================================================================
  // THE CONVERSION PIPELINE ENGINE METHOD
  // =========================================================================
  def updateMenu(contributorOpt: Option[MenuContributor]): Unit =
    // Wipe baseline menus back to pristine snapshots
    baseMenus.foreach: menu =>
      val initialCount = initialItemCounts.getOrElse(menu, 0)
      clearAddedItemsAndSubMenus(menu, initialCount)

    // Clear custom top-level column headers
    menus = baseMenus.toList

    // Convert backend structures into physical controls
    contributorOpt.foreach: contributor =>
      contributor.getContributedActions.foreach:
        // Scenario A: Render a standard clickable MenuItem control
        case action: MenuActionDefinition =>
          val targetMenu = resolveMenuPath(action.menuPath)
          val menuItem = getOrAddMenuItem(targetMenu, action.itemLabel): () =>
            contributor.handleMenuAction(action.actionId)

          menuItem.disableProperty().unbind()
          menuItem.disableProperty().bind(!contributor.getActionEnabledProperty(action.actionId))

          action.shortcut.foreach: sc =>
            val comboString = s"${sc.modifiers.mkString("+")}+${sc.key}"
            menuItem.accelerator = KeyCombination.keyCombination(comboString)

        // Scenario B: Render a native visual divider line
        case separator: MenuSeparatorDefinition =>
          val targetMenu = resolveMenuPath(separator.menuPath)

          // Instantiate and append a native UI divider item directly to the layout node target
          targetMenu.items.add(new SeparatorMenuItem())
  end updateMenu

  // =========================================================================
  // PATH RESOLUTION LOGIC (Your original architecture)
  // =========================================================================
  // Update this method inside your ContextBasedMenuBar class layout
  def getOrAddMenu(menuTitle: String)(action: () => Unit = () => ()): Menu =
    // Clean the lookup parameters to ignore mnemonic prefixes
    val cleanSearchTitle = menuTitle.replace("_", "")

    menus.find: existingMenu =>
      existingMenu.text.value.replace("_", "") == cleanSearchTitle
    .map(delegate => new Menu(delegate)).getOrElse:
      val newMenu: Menu = new Menu(menuTitle):
        mnemonicParsing = true

      val currentMenus = menus.toList.map(_.asInstanceOf[Menu])
      menus = currentMenuInsert(currentMenus, newMenu)
      newMenu

  end getOrAddMenu

  def resolveMenuPath(path: String, separator: String = "/"): Menu =
    val segments = path.split(separator).map(_.trim).filter(_.nonEmpty).toList
    segments match
      case Nil => throw new IllegalArgumentException("Menu path cannot be empty")
      case rootName :: subSegments =>
        val rootMenu = getOrAddMenu(rootName)()
        resolveSubMenuPath(rootMenu, subSegments)

  @tailrec
  private def resolveSubMenuPath(currentParent: Menu, remainingSegments: List[String]): Menu =
    remainingSegments match
      case Nil => currentParent
      case nextSegment :: tail =>
        val nextSubMenu = getOrAddSubMenu(currentParent, nextSegment)
        resolveSubMenuPath(nextSubMenu, tail)

  def getOrAddSubMenu(parentMenu: Menu, subMenuTitle: String): Menu =
    parentMenu.items.find(item => item.isInstanceOf[javafx.scene.control.Menu] && item.text.value == subMenuTitle)
      .map(item => new Menu(item.asInstanceOf[javafx.scene.control.Menu]))
      .getOrElse {
        val newSubMenu: Menu = new Menu(subMenuTitle) { mnemonicParsing = true }
        parentMenu.items.add(newSubMenu)
        newSubMenu
      }

  def getOrAddMenuItem(targetMenu: Menu, itemLabel: String)(action: () => Unit = () => ()): MenuItem =
    targetMenu.items.find(_.text.value == itemLabel)
      .map(item => new MenuItem(item))
      .getOrElse {
        val newMenuItem: MenuItem = new MenuItem(itemLabel) {
          mnemonicParsing = true
          onAction = (_: ActionEvent) => action()
        }
        targetMenu.items.add(newMenuItem)
        newMenuItem
      }

  private def currentMenuInsert(current: List[Menu], newMenu: Menu): List[Menu] =
    if current.length >= 2 then
      val heading = current.init
      val trailing = current.last
      heading ++ List(newMenu, trailing)
    else
      current :+ newMenu


  // =========================================================================
  // ENCAPSULATED TAIL-RECURSIVE DEEP CLEANUP STRATEGY
  // =========================================================================
  private def clearAddedItemsAndSubMenus(menu: Menu, initialNumberOfItems: Int): Unit =

    @tailrec
    def cleanupLoop(todo: List[(Menu, Int)]): Unit =
      todo match
        case Nil => () // Stack is empty, deep cleanup finished!
        case (currentMenu, initialCount) :: rest =>
          // 1. Scan the current menu's valid base items to collect nested sub-menus
          val nestedSubMenus = currentMenu.items.take(initialCount).collect:
            case subMenu: javafx.scene.control.Menu =>
              (new Menu(subMenu), subMenu.getItems.size)

          // 2. Perform loop-free index pruning via the nested helper
          pruneDynamicItems(currentMenu, initialCount)

          // 3. Continue traversing the stack with the remaining items
          cleanupLoop(nestedSubMenus.toList ++ rest)

    @tailrec
    def pruneDynamicItems(targetMenu: Menu, targetSize: Int): Unit =
      if targetMenu.items.size <= targetSize then () // Target reached!
      else
        val indexToRemove = targetMenu.items.size - 1
        targetMenu.items(indexToRemove) match
          case dynamicItem: javafx.scene.control.MenuItem =>
            dynamicItem.acceleratorProperty().unbind() // Prevent global shortcut leaks
          case null => ()

        targetMenu.items.remove(indexToRemove)
        pruneDynamicItems(targetMenu, targetSize) // Recursive jump instruction

    // Kick off the tree traversal starting with the parent menu node framework
    cleanupLoop(List((menu, initialNumberOfItems)))

  end clearAddedItemsAndSubMenus


end ContextBasedMenuBar
