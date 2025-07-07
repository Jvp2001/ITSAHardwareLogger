package org.itsadigitaltrust.hardwarelogger.views.tabs

import javafx.scene.control.cell
import org.itsadigitaltrust.hardwarelogger.delegates.{TabDelegate, TableRowDelegate}
import org.itsadigitaltrust.hardwarelogger.models.HardDriveModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.HardDriveTableRowViewModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.tabs.{HardDrivesTabViewModel, TabTableViewModel}
import org.itsadigitaltrust.hardwarelogger.core.ui.*
import org.itsadigitaltrust.hardwarelogger.views.View

import scalafx.application.Platform
import scalafx.event.EventType

class HardDriveTableView(using itsaID: String, tabViewModel: HardDrivesTabViewModel) extends TabTableView(using tabViewModel, itsaID):

  requestFocus()

  override val rowDelegate: Option[TableRowDelegate[HardDriveTableRowViewModel]] = Option(tabViewModel.rowDelegate)

  override val showHandCursorOnHover: Boolean = true

  private val healthColumn = createAndAddColumn[String]("Health"): cellValue =>
    StringProperty(cellValue.healthProperty.get.toString)

  private val performanceColumn = createAndAddColumn[String]("Performance"): cellValue =>
    StringProperty(cellValue.performanceProperty.get.toString)


  private val sizeColumn = createAndAddColumn[String]("Size"): cellValue =>
    cellValue.sizeProperty


  private val modelColumn = createAndAddColumn("Model", minWidth = ColumnSize.massive): cellValue =>
    cellValue.modelProperty

  private val serialColumn = createAndAddColumn("Serial", minWidth = ColumnSize.big): cellValue =>
    cellValue.serialProperty

  private val typeColumn = createAndAddColumn("Type"): cellValue =>
    cellValue.typeProperty

  private val idColumn = createAndAddColumn("ID"): cellValue =>
    cellValue.idProperty

  private val isSSDColumn = createAndAddColumn[String]("Is SSD"): cellValue =>
    cellValue.driveTypeProperty

  
end HardDriveTableView


class HardDrivesTabView(using itsaID: String) extends VBox with TabDelegate with View[HardDrivesTabViewModel]:
  override given viewModel: HardDrivesTabViewModel = new HardDrivesTabViewModel

  private val tableView = new HardDriveTableView()


  children += tableView
  children += new HBox:
    padding = Insets(5D, 5D, 0D, 0D)
    private val region = new Region:
      hgrow = Always
      prefHeight = 40
    private val moreInfoButton = new Button:
      text = "More Info"
      onAction = _ => viewModel.showExtraInfo(tableView.getSelectedItem)
      disable <== !viewModel.moreInfoDisabledProperty
      padding = Insets(0D, 5D, 0D, 0D)
      prefHeight = 40D
      prefWidth = 100D
      hgrow = Always
    children ++= Seq(region, moreInfoButton)


  def selectRow(index: Int = 0): Unit =
    tableView.getSelectionModel.select(index)


  override def onSelected(tab: Tab): Unit =
    selectRow()
end HardDrivesTabView








