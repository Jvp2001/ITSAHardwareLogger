package org.itsadigitaltrust.hardwarelogger.views.tabs

import javafx.event.EventHandler
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.control.{TableCell, cell}
import javafx.scene.{AccessibleAttribute, control, control as jfxsc, input as jfxi}
import javafx.util.{Callback, converter}
import org.itsadigitaltrust.common.Operators.{?, ??, |>, ||>}
import org.itsadigitaltrust.hardwarelogger.contextbasedemnubar.{MenuContributor, MenuManagerService}
import org.itsadigitaltrust.hardwarelogger.core.ui.*
import org.itsadigitaltrust.hardwarelogger.delegates.{ProgramMode, TabDelegate, TableEvent}
import org.itsadigitaltrust.hardwarelogger.models.HardDriveModel
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.ProgramModeChangeNotificationListener
import org.itsadigitaltrust.hardwarelogger.services.{HLDatabaseService, HardwareGrabberService, HardwareIDValidationService}
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.HardDriveTableRowViewModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.tabs.{HardDrivesHardModeTestingHelperViewModel, HardDrivesTabViewModel}
import org.itsadigitaltrust.hardwarelogger.views.{View, createAndAddColumn}
import scalafx.application.Platform
import scalafx.beans.binding.Bindings
import scalafx.beans.value.ObservableValue
import scalafx.scene.control.{ContextMenu, MenuBar, MenuItem, SeparatorMenuItem, TableSelectionModel, Tooltip}
import scalafx.scene.paint.Color
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

private trait HardDriveModeTestingHelper(using hardDriveTabViewModel: HardDrivesHardModeTestingHelperViewModel) extends MenuContributor:
  self: HardDriveTableView =>

end HardDriveModeTestingHelper

class HardDriveTableView(using tabViewModel: HardDrivesTabViewModel) extends TabTableView[HardDrivesTabViewModel#Model, HardDrivesTabViewModel#RowViewModel, HardDrivesTabViewModel], HardDriveModeTestingHelper:

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
      selectedProperty().onChange: (_, _, _) =>
        val rowItemProperty = getTableRow.itemProperty
        this.styleProperty() <== when(rowItemProperty.isNotNull && rowItemProperty.get.hasAValidIDProperty) `choose`
          "-fx-text-fill: -color-success-fg" `otherwise`
          "-fx-text-fill: -color-danger-fg"

      /*
      * NOTE: We use an Event Filter (Capturing phase) instead of an Event Handler (Bubbling phase).
      *
      * WHY?
      * The default JavaFX TableView behaviour intercepts the Space key during the capturing cycle
      * to trigger edit mode. However, it mistakenly passes the space character into the newly opened
      * TextField, wiping out the cell's existing text string.
      *
      * HOW THIS FIXES IT:
      * By registering an Event Filter, this cell intercepts the Space keypress *before* the parent
      * TableView can process it. Calling `event.consume()` stops the event from propagating further,
      * allowing us to trigger a safe `startEdit()` that leaves the existing text field intact.
      */
      addEventFilter(KeyEvent.KeyPressed, (event: javafx.scene.input.KeyEvent) =>
        if event.getCode == KeyCode.Space && !isEditing then
          event.consume()   // Intercept and kill the destructive default TableView shortcut
          startEdit()  // Cleanly enter edit mode manually
      )

      override def startEdit(): Unit =
        if tabViewModel.isCellEditable(selectionModel.value.getSelectedItem) then
          super.startEdit()
          // Capture the exact text value from the underlying model before JavaFX mutates anything
          val oldText = getItem
          // Locate the inner graphic TextField generated by JavaFX's native architecture
          getGraphic match
            case txtField: jfxsc.TextField =>
              // Force the text field back to the verified original string state
              txtField.setText(oldText)
              txtField.selectAll()
              txtField.requestFocus()

              // Add an inline filter to the inner field so spaces aren't treated as a clear event
              txtField.addEventFilter(KeyEvent.KeyPressed, (e: javafx.scene.input.KeyEvent) =>
                if (e.getCode == KeyCode.Space && txtField.getText == " ") then
                  // If the text field opened and contains only JavaFX's broken space payload, restore it
                  txtField.setText(oldText)
                  e.consume()

                )
            case _ => ()
    end new
  )
  idColumn.setOnEditCommit: event =>
    if tabViewModel.isCellEditable(selectionModel.value.getSelectedItem) then
      val tablePosition = event.getTablePosition
      val cell = event.getTableView.queryAccessibleAttribute(AccessibleAttribute.CELL_AT_ROW_COLUMN, tablePosition.getRow, tablePosition.getColumn).asInstanceOf[TableCell[HardDriveTableRowViewModel, String]]
      tabViewModel.delegate.foreach(_.onCellValueChanged(TableEvent.CellValueChanged("ID", event.getRowValue.?, event.getNewValue.?, cell.getStyleClass )))
  private val isSSDColumn = createAndAddColumn("Is SSD"): cellValue =>
    cellValue.driveTypeProperty


  editable <== tabViewModel.editableProperty


end HardDriveTableView


class HardDrivesTabView(using viewModel: HardDrivesTabViewModel) extends BorderPane, View[HardDrivesTabViewModel]:
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








