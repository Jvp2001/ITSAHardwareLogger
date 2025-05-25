package org.itsadigitaltrust.hardwarelogger.views.tabs

import org.itsadigitaltrust.hardwarelogger.services.HardwareGrabberService
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.MemoryTableRowViewModel
import org.itsadigitaltrust.hardwarelogger.core.ui.*
import org.itsadigitaltrust.hardwarelogger.delegates.TabDelegate
import org.itsadigitaltrust.hardwarelogger.models.MemoryModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.tabs.{MemoryTabViewModel, TabTableViewModel}
import scalafx.Includes.*
import scalafx.beans.property.IntegerProperty


final class MemoryTabView(using itsaID: String = "") extends VBox:
  given viewModel: MemoryTabViewModel = new MemoryTabViewModel

  private val totalMemLabel = new Label:
    text = "Total Memory"
    margin = Insets(0.0, 10.0, 0.0, 10.0)

  private val totalMemValueLabel = new Label:
    text <== viewModel.totalMemoryProperty


  private val totalMemoryContainer = new HBox:
    prefHeight = 20.0
    prefWidth = 200.0
    margin = Insets(10.0, 0, 0, 0)
    children = Seq(totalMemLabel, totalMemValueLabel)


  private val tableView = new TabTableView[MemoryModel, MemoryTableRowViewModel]()
    //    minWidth = Double.MaxValue
    //    minHeight = Double.MaxValue



  import org.itsadigitaltrust.hardwarelogger.core.BeanConversions.given

  private val sizeColumn = tableView.createAndAddColumn("Size"): cellValue =>
    cellValue.sizeProperty

  private val descriptionColumn = tableView.createAndAddColumn("Description"): cellValue =>
    cellValue.descriptionProperty


  spacing = 10.0
  children = Seq(
    totalMemoryContainer,
    tableView
  )

end MemoryTabView

