package org.itsadigitaltrust.hardwarelogger.views.tabs

import org.itsadigitaltrust.hardwarelogger.core.ui.*
import org.itsadigitaltrust.hardwarelogger.viewmodels.{TabTableViewModel, TableRowViewModel}
import scalafx.beans.property.{ObjectProperty, Property, StringProperty}
import scalafx.beans.value.ObservableValue
import scalafx.scene.control.{TableColumn, TableView}





class TabTableView[M, T <: TableRowViewModel[M]](using viewModel: TabTableViewModel[M, T]) extends TableView[T]:
  import org.itsadigitaltrust.hardwarelogger.core.BeanConversions.given

  vgrow = Always
  items = viewModel.data
  class TableTabColumn[P] extends TableColumn[T, P] // Placeholder for your custom TableTabColumn class

  def createAndAddColumn[P, J](
                             name: String,
                             minWidth: Int = 50)
                           (
                             cellValueFactory: T => ObservableValue[P, P]
                           ): TableTabColumn[P] =
    val column = createColumn(name, minWidth)(cellValueFactory)
    columns += column
    column


  def createColumn[P, J](
                       name: String,
                       minWidth: Int = 50)
                     (
                       cellValueFactory: T => ObservableValue[P, P]
                     ): TableTabColumn[P] =

    val column = new TableTabColumn[P]()
    setupColumn(column, name, minWidth, cellValueFactory)
    column
  end createColumn


  def setupColumn[P, J](
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