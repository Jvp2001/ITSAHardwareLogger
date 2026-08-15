package org.itsadigitaltrust.hardwarelogger.views.tabs

import javafx.event.EventHandler
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.control.{TableCell, cell}
import javafx.scene.{control, input as jfxi, control as jfxsc}
import javafx.util.{Callback, converter}
import org.itsadigitaltrust.common.Operators.{??, |>, ||>}
import org.itsadigitaltrust.hardwarelogger.core.ui.*
import scalafx.Includes.*
import org.itsadigitaltrust.hardwarelogger.delegates.{ProgramMode, TabDelegate, TableRowDelegate}
import org.itsadigitaltrust.hardwarelogger.models.HardDriveModel
import org.itsadigitaltrust.hardwarelogger.services.{HLDatabaseService, HardwareGrabberService}
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.HardDriveTableRowViewModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.tabs.HardDrivesTabViewModel
import org.itsadigitaltrust.hardwarelogger.views.{View, createAndAddColumn}
import scalafx.Includes.*
import scalafx.application.Platform
import scalafx.beans.value.ObservableValue
import scalafx.scene.control.{ContextMenu, MenuItem, TableSelectionModel, Tooltip}
import scalafx.util.StringConverter
import scalafx.util.converter.DefaultStringConverter

import scala.jdk.javaapi.CollectionConverters.asJavaCollection
import scala.ref.WeakReference
import scala.reflect.classTag


class ItsaIDConverter(model: HardDriveTableRowViewModel) extends StringConverter[String]:
  override def fromString(string: String): String =
    model.model.itsaID = string
    model.model.itsaID

  override def toString(t: String): String =
    t


class HardDriveTableView(tabViewModel: HardDrivesTabViewModel)(using applicationState: HardwareLoggerApplicationState)(using HardwareGrabberService) extends TabTableView[HardDriveModel, HardDriveTableRowViewModel]:


  requestFocus()

  HardDriveTableRowViewModel.apply ||> applicationState.hardDrives ?? Seq.empty |> asJavaCollection |> items.value.setAll
  //  items.value.setAll(applicationState.hardDrives.map(_.map(HardDriveTableRowViewModel.apply)) ?? Seq() *)

  private given rowDelegate: Option[TableRowDelegate[HardDriveTableRowViewModel]] = Option(tabViewModel)
  //  override val rowDelegate: WeakReference[TableRowDelegate[HardDriveTableRowViewModel]] = tabViewModel.rowDelegate
  //
  //  override val showHandCursorOnHover: Boolean = true

  rowFactory = _ => new TableTabRow[HardDriveTableRowViewModel](true):
    contextMenu =
      val moreInfoMenuItem = new MenuItem("More Info (i)"):
        onMouseClicked = (event: jfxi.MouseEvent) =>
          if event.getButton == jfxi.MouseButton.PRIMARY then
            tabViewModel.showExtraInfo(selectionModel.value.getSelectedItem)

      new ContextMenu(moreInfoMenuItem)


  private val healthColumn = createAndAddColumn("Health"): cellValue =>
    StringProperty(cellValue.healthProperty.get)

  private val performanceColumn = createAndAddColumn[String]("Performance"): cellValue =>
    StringProperty(cellValue.performanceProperty.get)


  private val sizeColumn = createAndAddColumn[String]("Size"): cellValue =>
    cellValue.sizeProperty


  private val modelColumn = createAndAddColumn("Model", ColumnSize.massive): cellValue =>
    cellValue.modelProperty

  private val serialColumn = createAndAddColumn("Serial", ColumnSize.big): cellValue =>
    cellValue.serialProperty

  private val typeColumn = createAndAddColumn("Type"): cellValue =>
    cellValue.typeProperty

  private val idColumn = createAndAddColumn("ID"): cellValue =>
    cellValue.idProperty

  idColumn.setCellFactory((param: control.TableColumn[HardDriveTableRowViewModel, String]) =>
    new TextFieldTableCell[HardDriveTableRowViewModel, String](new converter.DefaultStringConverter):
      override def startEdit(): Unit =
        if tabViewModel.isCellEditable(selectionModel.value.getSelectedItem) then
          super.startEdit()

  )

  private val isSSDColumn = createAndAddColumn[String]("Is SSD"): cellValue =>
    cellValue.driveTypeProperty


end HardDriveTableView


class HardDrivesTabView(using databaseService: HLDatabaseService, hardwareGrabberService: HardwareGrabberService, applicationState: HardwareLoggerApplicationState) extends BorderPane with TabDelegate with View[HardDrivesTabViewModel]:
  private val tableView = new HardDriveTableView(viewModel)

  override given viewModel: HardDrivesTabViewModel = new HardDrivesTabViewModel

  requestFocus()

  center = tableView
  bottom = new HBox:
    minHeight = 50D
    prefHeight = 50D
    padding = Insets(5D, 5D, 0D, 0D)
    private val region = new Region:
      hgrow = Always
    private val moreInfoButton = new Button:
      alignment = Pos.CenterRight
      text = "More Info (i)"
      onAction = _ =>
        viewModel.showExtraInfo(tableView.selectionModel().getSelectedItem)
      disable <== viewModel.moreInfoDisabledProperty




    children ++= Seq(region, moreInfoButton)


  def selectRow(index: Int = 0): Unit =
    Platform.runLater:
      tableView.getSelectionModel.select(index)
      tableView.requestFocus()
      tableView.scrollTo(index)


  override def onSelected(tab: Tab): Unit =

    selectRow()

  
  tableView.addEventFilter(scalafx.scene.input.KeyEvent.KeyReleased, (t: KeyEvent) => 
    viewModel.onRowKeyboardKeyReleased(t, tableView.getSelectionModel.selectedItemProperty().get() |> Option.apply)
  )





end HardDrivesTabView








