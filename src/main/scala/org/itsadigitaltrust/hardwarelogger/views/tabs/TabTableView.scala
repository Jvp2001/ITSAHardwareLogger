package org.itsadigitaltrust.hardwarelogger.views.tabs

import org.itsadigitaltrust.hardwarelogger.core.ui.*
import org.itsadigitaltrust.hardwarelogger.delegates.{TabDelegate, TableRowDelegate}
import org.itsadigitaltrust.hardwarelogger.viewmodels.TableRowViewModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.tabs.TabTableViewModel
import scalafx.beans.value.ObservableValue
import scalafx.Includes.{*, given}
import scalafx.scene.control.{TableColumn, TableRow, TableView}
import javafx.scene.control as jfxsc


class TabTableView[M, T <: AnyRef & TableRowViewModel[M] | AnyRef & TableRowViewModel[M] & TableRowDelegate[TableRowViewModel[M]]](using viewModel: TabTableViewModel[M, T]) extends TableView[T] with TabDelegate:

  var rowDelegate: Option[TableRowDelegate[T]] = None
  
  
  private class TableTabRow[R](
                        var rowDelegate: Option[TableRowDelegate[R]]) extends jfxsc.TableRow[R]:

    override def updateItem(item: R, empty: Boolean): Unit =
      super.updateItem(item, empty)
      if empty || item == null then
        setGraphic(null)
      else if rowDelegate.isDefined then

        rowDelegate.get.onUpdateItem(Option(item))
    end updateItem

    setOnMouseClicked: event =>
      if event.getClickCount == 1 then
        if rowDelegate.isDefined then
          rowDelegate.get.onSelected(Option(getItem))


    override def updateSelected(b: Boolean): Unit = 
      super.updateSelected(b)
      if rowDelegate.isDefined then
        val item = Option(getItem)
        rowDelegate.get.onSelected(item)

    end updateSelected
  end TableTabRow


  class TableTabColumn[P] extends TableColumn[T, P] // Placeholder for your custom TableTabColumn class

  rowFactory = _ => new TableTabRow[T](rowDelegate)

  vgrow = Always
  items = viewModel.data
  tableMenuButtonVisible = true


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
    column.setReorderable(true)
    column.text = name
    column.minWidth = minWidth
    column.sortable = false

    column.cellValueFactory = p =>
      cellValueFactory(p.getValue)
  end setupColumn


end TabTableView