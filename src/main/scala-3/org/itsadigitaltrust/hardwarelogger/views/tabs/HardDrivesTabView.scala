package org.itsadigitaltrust.hardwarelogger.views.tabs

import javafx.scene.control.cell
import org.itsadigitaltrust.hardwarelogger.delegates.{TabDelegate, TableRowDelegate}
import org.itsadigitaltrust.hardwarelogger.models.HardDriveModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.HardDriveTableRowViewModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.tabs.{HardDrivesTabViewModel, TabTableViewModel}
import scalafx.beans.property.{BooleanProperty, DoubleProperty, ObjectProperty, StringProperty}
import org.itsadigitaltrust.hardwarelogger.core.ui.*
import org.itsadigitaltrust.hardwarelogger.views.View
import scalafx.application.Platform
import scalafx.scene.control.cell.CheckBoxTableCell

class HardDriveTableView(using itsaID: String, tabViewModel: HardDrivesTabViewModel ) extends TabTableView( using tabViewModel, itsaID):
  
  requestFocus()


  showHandCursorOnHover = true

  private val healthColumn = createAndAddColumn[String]("Health"): cellValue =>
    StringProperty(cellValue.healthProperty.get.toString)

  private val performanceColumn = createAndAddColumn[String]("Performance"): cellValue =>
    StringProperty(cellValue.performanceProperty.get.toString)



  private val sizeColumn = createAndAddColumn[String]("Size"): cellValue =>
    cellValue.sizeProperty


  private val modelColumn = createAndAddColumn("Model"): cellValue =>
    cellValue.modelProperty

  private val serialColumn = createAndAddColumn("Serial"): cellValue =>
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
  children += new VBox():
    children += new StackPane()
      styleClass ++= List("hdsentinel-background")
      private val text = new Label("HDSentinel text here"):
        this.text <== viewModel.description
        styleClass ++= List("hdsentinel-text")
      children += text
      children += new VBox(10):
        children += text
        children += new Label("No actions needed."):
          this.text <== viewModel.actionsText
          styleClass ++= List("hdsentinel-text")
      vgrow = Always

  private val infoBox = new GridPane(10, 10):
    private val powerOnTimeNameLabel = new Label("Power On Time:"):
      styleClass ++= List("name-label", "hdsentinel-text")
    private val powerOnTimeValueLabel = new Label:
      text <== viewModel.powerOnTime
      styleClass ++= List("value-label", "hdsentinel-text")
    private val estimatedLifeTimeLabel = new Label("Estimated reaming lifetime:"):
      styleClass ++= List("name-label", "hdsentinel-text")
    private val estimatedLifeTimeValueLabel = new Label:
      text <== viewModel    .estimatedLifeTime
      styleClass ++= List("value-label", "hdsentinel-text")

    addRow(0, powerOnTimeNameLabel, powerOnTimeValueLabel)
    addRow(1, estimatedLifeTimeLabel, estimatedLifeTimeValueLabel)
  end infoBox
  children += infoBox

  def selectRow(index: Int = 0): Unit =
    tableView.getSelectionModel.select(index)

  override def onSelected(tab: Tab): Unit =
    selectRow()
end HardDrivesTabView








