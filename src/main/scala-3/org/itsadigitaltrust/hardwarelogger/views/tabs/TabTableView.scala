package org.itsadigitaltrust.hardwarelogger.views.tabs

import org.itsadigitaltrust.common.Operators.|>

import org.itsadigitaltrust.hardwarelogger.core.ui.*
import org.itsadigitaltrust.hardwarelogger.delegates.{TabDelegate, TableRowDelegate}
import org.itsadigitaltrust.hardwarelogger.viewmodels.TableRowViewModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.tabs.TabTableViewModel

import javafx.beans.value
import javafx.beans.value.ChangeListener
import javafx.event.EventHandler
import scalafx.beans.value.ObservableValue
import scalafx.Includes.{*, given}
import scalafx.scene.control.{TableColumn, TableRow, TableView}
import javafx.scene.control as jfxsc
import scalafx.event.EventType
import scalafx.event.subscriptions.Subscription
import scalafx.scene.Cursor
import scalafx.scene.input.KeyEvent


private class TableTabRow[R](val showHandCursorOnHover: Boolean)(using rowDelegate: Option[TableRowDelegate[R]]) extends jfxsc.TableRow[R]:

  setOnMouseClicked(event =>
    if event.clickCount == 2 then
      rowDelegate.map: r =>
        val item1 = getItem
        r.onRowDoubleClicked(event.getButton, Option(item1))
  )


  hoverProperty().addListener: (_, _, newValue) =>
    if showHandCursorOnHover then
      if newValue then
        setCursor(Cursor.Hand)
      else
        setCursor(Cursor.Default)

  override def updateItem(item: R, empty: Boolean): Unit =
    super.updateItem(item, empty)
    if empty || item == null then
      setGraphic(null)
    else if rowDelegate.isDefined then
      rowDelegate.get.onUpdateItem(Option(item))
  end updateItem

  setOnMouseClicked: event =>
    if event.getClickCount == 2 then
      if rowDelegate.isDefined then
        rowDelegate.get.onRowDoubleClicked(event.getButton, Option(getItem))


  override def updateSelected(b: Boolean): Unit =
    super.updateSelected(b)
    if rowDelegate.isDefined then
      val item = Option(getItem)
      rowDelegate.get.onSelected(item)
  end updateSelected
end TableTabRow

abstract class TabTableView[M, T <: TableRowViewModel[M]](using viewModel: TabTableViewModel[M, T], itsaID: String) extends TableView[T]:

  val vm: TabTableViewModel[M, T] = viewModel
  def getViewModel = viewModel
  val rowDelegate: Option[TableRowDelegate[T]] = None
  val showHandCursorOnHover: Boolean = false
  val reordableColumns: Boolean = false
  val triggerDbClickedWhenEnterIsPressedInNonEditingTable: Boolean = true

  val nonEditableSelectionConfirmation = (event: KeyEvent) =>
    if triggerDbClickedWhenEnterIsPressedInNonEditingTable && event.code == KeyCode.Space || event.code == KeyCode.Enter then
      rowDelegate.foreach(_.onRowDoubleClicked(MouseButton.Primary, Option(getSelectedItem)))
    end if
  end nonEditableSelectionConfirmation

  private val keyEventFilter: Option[Subscription] = filterEvent(KeyEvent.Any): (event: KeyEvent) =>
    event.eventType match
      case _: EventType[KeyEvent.KeyPressed.type] if !editable.value =>
        nonEditableSelectionConfirmation(event)
  |> Option[Subscription]
  end keyEventFilter

  editable = false





  class TableTabColumn[P] extends TableColumn[T, P]

  rowFactory = _ => new TableTabRow[T](showHandCursorOnHover)(using rowDelegate)

  vgrow = Always
  items = viewModel.data
  tableMenuButtonVisible = true


  def getSelectedItem: T = selectionModel.apply().getSelectedItem
  def createAndAddColumn[P](
                             name: String,
                             minWidth: Int = 50)
                           (
                             cellValueFactory: T => ObservableValue[P, P]
                           ): TableTabColumn[P] =
    val column = createColumn(name, minWidth)(cellValueFactory)
    columns += column
    column


  def createColumn[P](
                       name: String,
                       minWidth: Int = 50)
                     (
                       cellValueFactory: T => ObservableValue[P, P]
                     ): TableTabColumn[P] =

    val column = new TableTabColumn[P]()
    setupColumn(column, name, minWidth, cellValueFactory)
    column
  end createColumn


  def setupColumn[P](
                      column: TableTabColumn[P],
                      name: String,
                      minWidth: Int = 50,
                      cellValueFactory: T => ObservableValue[P, P]
                    ): Unit =
    column.editable = false
    column.setReorderable(reordableColumns)
    column.text = name
    column.minWidth = minWidth
    column.sortable = false

    column.cellValueFactory = p =>
      cellValueFactory(p.getValue)
  end setupColumn

  selectionModel.apply().selectedItem.onChange: (op, newValue, oldValue) =>
    rowDelegate.foreach(_.onSelected(selectionModel.apply().getSelectedItem |> Option[T] ))



end TabTableView