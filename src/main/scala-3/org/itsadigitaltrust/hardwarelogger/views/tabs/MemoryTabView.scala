package org.itsadigitaltrust.hardwarelogger.views.tabs

import org.itsadigitaltrust.common.Operators.{??, |>, ||>}
import org.itsadigitaltrust.hardwarelogger.contextbasedemnubar.MenuManagerService
import org.itsadigitaltrust.hardwarelogger.core.ui.*
import org.itsadigitaltrust.hardwarelogger.models.MemoryModel
import org.itsadigitaltrust.hardwarelogger.services.HardwareGrabberService
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.MemoryTableRowViewModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.tabs.MemoryTabViewModel
import org.itsadigitaltrust.hardwarelogger.views.View

import scala.jdk.javaapi.CollectionConverters


final class MemoryTabView(using hardwareGrabberService: HardwareGrabberService, menuManagerService: MenuManagerService)(using viewModel: MemoryTabViewModel = new MemoryTabViewModel) extends VBox, View[MemoryTabViewModel](using viewModel):

  private val totalMemLabel = new Label:
    text = "Total Memory"
    margin = Insets(0.0, 10.0, 0.0, 10.0)
    styleClass ++= Seq("memory-label", "large-text")
  
  private val totalMemValueLabel = new Label:
    text() = viewModel.totalMemoryValue
    styleClass ++= Seq("memory-label", "large-text")
  
  
  private val totalMemoryContainer = new HBox:
    prefHeight = 20.0
    prefWidth = 200.0
    margin = Insets(10.0, 0, 0, 0)
    children = Seq(totalMemLabel, totalMemValueLabel)
    styleClass += "total-memory-container"
  
  children += totalMemoryContainer
  children += new TabTableView[MemoryTabViewModel#Model, MemoryTabViewModel#RowViewModel, MemoryTabViewModel]:
    vgrow = Always
//    items.value.setAll(sharedApplicationData.memory.map(_.map(MemoryTableRowViewModel.apply)) ?? Seq() *)
//    MemoryTableRowViewModel.apply
//      ||> sharedApplicationData.memory ?? Seq.empty
//      |> CollectionConverters.asJavaCollection
//      |> items.value.setAll
    columns ++= Seq(
      new TableColumn[MemoryTableRowViewModel, String]:
        text = "Size"
        minWidth = 50
        cellValueFactory = _.value.sizeProperty
      ,
      new TableColumn[MemoryTableRowViewModel, String]:
        text = "Description"
        minWidth = 50
        cellValueFactory = _.value.descriptionProperty
      ,
      new TableColumn[MemoryTableRowViewModel, String]:
        text = "Type"
        minWidth = 50
        cellValueFactory = _.value.typeProperty
    )
  
    spacing = 10.0
 

end MemoryTabView

