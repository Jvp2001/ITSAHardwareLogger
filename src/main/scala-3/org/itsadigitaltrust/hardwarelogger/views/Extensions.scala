package org.itsadigitaltrust.hardwarelogger.views
import org.itsadigitaltrust.common.Operators.|>
import org.itsadigitaltrust.hardwarelogger.core.ui.*
import org.itsadigitaltrust.hardwarelogger.models.HLModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.TableRowViewModel
import scalafx.beans.value.ObservableValue


extension[ S <: TableRowViewModel[? <: HLModel]](tableView: TableView[S])

  private def createAndAddColumn[P](name: String, minimumWidth: Double = 50D )(f: S=> ObservableValue[P, P]): TableColumn[S, P] =
    val column = new TableColumn[S, P]():
      text = name
      minWidth = minimumWidth
      cellValueFactory = _.value |> f
    tableView.columns += column
    column