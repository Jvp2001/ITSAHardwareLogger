package org.itsadigitaltrust.hardwarelogger.views.tabs
import org.itsadigitaltrust.common.Operators.|>
import org.itsadigitaltrust.hardwarelogger.core.ui.*
import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.models.HLModel
import org.itsadigitaltrust.hardwarelogger.services.HardwareGrabberService
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.TableRowViewModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.tabs.TabTableViewModel
import scalafx.beans.value.ObservableValue

import scala.reflect.ClassTag

class HardwareLoggerTableView[M <: HLModel : ClassTag, T <: TableRowViewModel[M]]
(using applicationState: HardwareLoggerApplicationState)(using vm: TabTableViewModel[M, T], hardwareGrabberService: HardwareGrabberService) extends TableView[T]:
  type Column[P] = TableColumn[T, P]

  items = vm.data

  def createAndAddColumn[P](text: String, minimumWidth: Double = 50D)(f: T => ObservableValue[P, P]): TableColumn[T,P] =
    createAndAddCustomColumn[P](text): (col: TableColumn[T, P]) =>
      col.minWidth = minimumWidth
      col.cellValueFactory = _.value |> f
  end createAndAddColumn

  def createAndAddCustomColumn[P](text: String)(body: TableColumn[T, P] => Unit):  Column[P]  =
    val column = new TableColumn[T, P](text):
      editable = false
      body(this)
    columns += column
    column
  end createAndAddCustomColumn







