package org.itsadigitaltrust.hardwarelogger.delegates

import javafx.scene.input.KeyEvent
import scalafx.beans.binding.ObjectBinding
import scalafx.beans.property.{ObjectProperty, ReadOnlyObjectProperty}
import scalafx.scene.control.{TableRow, TableSelectionModel}
import scalafx.scene.input.{KeyCode, MouseButton}

trait TableRowDelegate[R <: Object]:

  val showHandCursorOnHover: Boolean = true



  def onSelected(row: Option[R]): Unit = ()
  def onUpdateItem(row: Option[R], tableRow: TableRow[R]): Unit = ()

  def onRowDoubleClicked(button: MouseButton, row: Option[R]): Unit = ()
  def onRowKeyboardKeyReleased(event: KeyEvent, row: Option[R]): Unit = ()
