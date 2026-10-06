package org.itsadigitaltrust.hardwarelogger.views.tabs

import javafx.event.EventHandler
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.control.{TableCell, cell}
import javafx.scene.{control, control as jfxsc, input as jfxi}
import javafx.util.{Callback, converter}
import org.itsadigitaltrust.common.Operators.{?, ??, |>, ||>}
import org.itsadigitaltrust.hardwarelogger.contextbasedemnubar.{MenuContributor, MenuManagerService}
import org.itsadigitaltrust.hardwarelogger.core.ui.*
import scalafx.Includes.*
import org.itsadigitaltrust.hardwarelogger.delegates.{ProgramMode, TabDelegate}
import org.itsadigitaltrust.hardwarelogger.models.HardDriveModel
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.ProgramModeChangeNotificationListener
import org.itsadigitaltrust.hardwarelogger.services.{HLDatabaseService, HardwareGrabberService}
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.HardDriveTableRowViewModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.tabs.{HardDrivesHardModeTestingHelperViewModel, HardDrivesTabViewModel}
import org.itsadigitaltrust.hardwarelogger.views.{View, createAndAddColumn}
import scalafx.Includes.*
import scalafx.application.Platform
import scalafx.beans.value.ObservableValue
import scalafx.scene.control.{ContextMenu, MenuBar, MenuItem, SeparatorMenuItem, TableSelectionModel, Tooltip}
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

private trait HardDriveModeTestingHelper(using menu: Menu, hardDriveTabViewModel: HardDrivesHardModeTestingHelperViewModel) extends MenuContributor:
  self: HardDriveTableView =>

end HardDriveModeTestingHelper

class HardDriveTableView(using tabViewModel: HardDrivesTabViewModel, menu: Menu)(using HardwareGrabberService, HardwareLoggerApplicationState) extends TabTableView[HardDrivesTabViewModel#Model, HardDrivesTabViewModel#RowViewModel, HardDrivesTabViewModel], HardDriveModeTestingHelper:

  stylesheets += stylesheet"extrainfodialog.css"
  requestFocus()

  userData = this.asInstanceOf[MenuContributor]

  private given rowDelegate: Option[org.itsadigitaltrust.hardwarelogger.delegates.TableDelegate[HardDriveTableRowViewModel]] = Option(tabViewModel)
  //  override val rowDelegate: WeakReference[TableDelegate[HardDriveTableRowViewModel]] = tabViewModel.rowDelegate
  //
  //  override val showHandCursorOnHover: Boolean = true



  private val healthColumn = createAndAddColumn("Health"): cellValue =>
    StringProperty(cellValue.healthProperty.get)

  private val performanceColumn = createAndAddColumn("Performance"): cellValue =>
    StringProperty(cellValue.performanceProperty.get)


  private val sizeColumn = createAndAddColumn("Size"): cellValue =>
    cellValue.sizeProperty


  private val modelColumn = createAndAddColumn("Model", ColumnSize.massive): cellValue =>
    cellValue.modelProperty

  private val serialColumn = createAndAddColumn("Serial", ColumnSize.big): cellValue =>
    cellValue.serialProperty

  private val typeColumn = createAndAddColumn("Type"): cellValue =>
    cellValue.typeProperty

  private val idColumn = createAndAddColumn("ID", ColumnSize.big): cellValue =>
    cellValue.idProperty



  idColumn.editable <== tabViewModel.editIDColumn



  
  idColumn.setCellFactory((param: control.TableColumn[HardDriveTableRowViewModel, String]) =>
    new TextFieldTableCell[HardDriveTableRowViewModel, String](new converter.DefaultStringConverter):
      override def startEdit(): Unit =
        if tabViewModel.isCellEditable(selectionModel.value.getSelectedItem) then
          super.startEdit()

  )

  private val isSSDColumn = createAndAddColumn("Is SSD"): cellValue =>
    cellValue.driveTypeProperty


  editable <== tabViewModel.editableProperty


end HardDriveTableView


class HardDrivesTabView(using menu: Menu)(using databaseService: HLDatabaseService, hardwareGrabberService: HardwareGrabberService, hardwareLoggerApplicationState: HardwareLoggerApplicationState, menuManagerService: MenuManagerService)(using viewModel: HardDrivesTabViewModel = new HardDrivesTabViewModel) extends BorderPane, View[HardDrivesTabViewModel]:
  private val tableView = new HardDriveTableView


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

  selectRow()



end HardDrivesTabView








