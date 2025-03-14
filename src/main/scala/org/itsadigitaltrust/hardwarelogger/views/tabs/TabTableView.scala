package org.itsadigitaltrust.hardwarelogger.views.tabs

import javafx.beans.property.*
import javafx.beans.value.ObservableValue
import javafx.scene.control.TableColumn.CellDataFeatures
import javafx.scene.control.{Tab, TableColumn, TableView}
import javafx.util.Callback
import org.itsadigitaltrust.hardwarelogger.viewmodels.{TabTableViewModel, TableRowViewModel}
import org.springframework.stereotype.{Component, Controller}


@Controller
class TabTableView[M, T <: TableRowViewModel[M]](val viewModel: TabTableViewModel[M, T]) extends TableView[T]:


  class TableTabColumn[P] extends TableColumn[T, P] // Placeholder for your custom TableTabColumn class

  def createAndAddColumn[P](
                             name: String,
                             minWidth: Int = 50)
                           (
                             cellValueFactory: T => ObservableValue[P]
                           ): TableTabColumn[P] =
    val column = createColumn(name, minWidth)(cellValueFactory)
    getColumns.add(column)
    column


  def createColumn[P](
                       name: String,
                       minWidth: Int = 50)
                     (
                       cellValueFactory: T => ObservableValue[P]
                     ): TableTabColumn[P] =

    val column = new TableTabColumn[P]()
    setupColumn(column, name, minWidth, cellValueFactory)
    column
  end createColumn


  def setupColumn[P](
                      column: TableTabColumn[P],
                      name: String,
                      minWidth: Int = 50,
                      cellValueFactory: T => ObservableValue[P]
                    ): Unit =
    column.setEditable(false)
    column.setReorderable(false)
    column.setText(name)
    column.setMinWidth(minWidth)
    column.setSortable(false)

    column.setCellValueFactory(new Callback[CellDataFeatures[T, P], ObservableValue[P]]:
      override def call(p: CellDataFeatures[T, P]): ObservableValue[P] =
        cellValueFactory(p.getValue)
      )
  end setupColumn


end TabTableView
