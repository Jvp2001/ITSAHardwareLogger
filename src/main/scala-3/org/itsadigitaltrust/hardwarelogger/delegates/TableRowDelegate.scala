package org.itsadigitaltrust.hardwarelogger.delegates

import scalafx.scene.control.TableRow
import scalafx.scene.input.{KeyCode, MouseButton}

trait TableRowDelegate[R]:
  def onSelected(row: Option[R]): Unit = ()
  def onUpdateItem(row: Option[R], tableRow: TableRow[R]): Unit = ()
  
  def onRowDoubleClicked(button: MouseButton, row: Option[R]): Unit = ()
  def onRowKeyboardKeyReleased(key: KeyCode, row: Option[R]): Unit = ()
