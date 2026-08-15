package org.itsadigitaltrust.hardwarelogger.viewmodels.tabs

import javafx.scene.control as jfxsc
import javafx.scene.control.{ButtonType, TableSelectionModel}
import org.itsadigitaltrust.hardwarelogger.delegates.{ProgramMode, TableRowDelegate}
import org.itsadigitaltrust.hardwarelogger.dialogs.Dialogs
import org.itsadigitaltrust.hardwarelogger.services.{HLDatabaseService, HardwareGrabberService}
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.HardDriveTableRowViewModel
import scalafx.beans.property.{BooleanProperty, ObjectProperty}
import scalafx.scene.control.{TableRow, TableView, TextInputDialog}
import scalafx.scene.input.{KeyCode, MouseButton}
import scalafx.Includes.*
import scalafx.scene.input.MouseButton.Primary
import ox.{either, pipe}
import either.ok
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState

import scala.compiletime.summonFrom
import scala.ref.WeakReference
import scala.util.boundary
import scala.util.boundary.break
import javafx.scene.input as jfxsi
import javafx.scene.input.KeyEvent
import org.itsadigitaltrust.common.Operators.|>
import scalafx.application.Platform

import scala.reflect.Selectable.reflectiveSelectable





final class HardDrivesTabViewModel(using databaseService: HLDatabaseService)(using HardwareGrabberService, HardwareLoggerApplicationState) extends TabTableViewModel(HardDriveTableRowViewModel.apply, _.hardDrives) with TableRowDelegate[HardDriveTableRowViewModel]:
  private final val unhealthyDriveClass = "unhealthy-drive"

  val editIDColumn = BooleanProperty(false)

  var moreInfoDisabledProperty: BooleanProperty = BooleanProperty(false)

  moreInfoDisabledProperty



  // When its value is changed, it will trigger the table view to edit this cell
  val editingCellProperty: ObjectProperty[Option[(row: Int, column: Int)]] = ObjectProperty(None)

  val editableProperty: BooleanProperty = BooleanProperty(false)
  editableProperty <== ProgramMode.isHardDriveMode

  override def onUpdateItem(row: Option[HardDriveTableRowViewModel], tableRow: TableRow[HardDriveTableRowViewModel]): Unit =
    moreInfoDisabledProperty.value = row.isEmpty
    tableRow.styleClass += row.map: rowModel =>
      rowModel.model.`type` match
        case "SSD" if rowModel.model.health.toByte < 50 => unhealthyDriveClass
        case "HDD" | "HHD" if rowModel.model.health.toByte < 100 => unhealthyDriveClass
        case _ => ""
    .get
  end onUpdateItem


  override def onRowKeyboardKeyReleased(event: KeyEvent, row: Option[HardDriveTableRowViewModel]): Unit =
    val tableViewOption = Option(event.getSource).collect { case tv: jfxsc.TableView[?] => tv }
    tableViewOption.foreach: tableView =>
      super.onRowKeyboardKeyReleased(event, row)
      if event.getCode.equals(jfxsi.KeyCode.I) then
        showExtraInfo(row.get)

      else if event.getCode.equals(jfxsi.KeyCode.ENTER) then
        event.consume()



        Platform.runLater:
          tableView.edit(tableView.selectionModel().getSelectedIndex,
            tableView.getColumns.stream().filter(c => c.getText `equals` "ID").findFirst().get)

      end if
  end onRowKeyboardKeyReleased




  override def onRowDoubleClicked(button: MouseButton, row: Option[HardDriveTableRowViewModel]): Unit =
    button match
      case Primary =>
        row.foreach: r =>
          if !editableProperty() then
            showExtraInfo(r)
          else
            ()

      case _ => ()

  end onRowDoubleClicked





  def showExtraInfo(r: HardDriveTableRowViewModel): Unit =
      Dialogs.showHardDriveExtraInfoDialog(r.model)


  override def onSelected(selectedRow: Option[HardDriveTableRowViewModel]): Unit =
    moreInfoDisabledProperty.value = selectedRow.isEmpty


  override def onProgramModeChanged(mode: ProgramMode): Either[Error, Unit] =
      editIDColumn() = ProgramMode.isInHardDriveMode
      Right(())

  override def save(): Either[Error, Unit] =
    ox.either:

      if models.isEmpty then
        logger.info("No models to save.")
        ()
      else
        models.map(databaseService.insertOrUpdateModel)
  end save

  def isCellEditable(row: HardDriveTableRowViewModel | Null): Boolean =
    row != null && editableProperty()

  extension (inline t: None.type)
    /** Unwrap the value of the `Option`, short-circuiting the computation to the enclosing [[either]] on guaranteed `None`. */
    transparent inline def failAndShow(): Nothing =
      summonFrom:
        case given boundary.Label[Either[Unit, Nothing]] => break(Left(()))
        case given boundary.Label[Either[Nothing, Nothing]] =>
          scala.compiletime.error(
            "The enclosing `either` call uses a different error type.\nIf it's explicitly typed, is the error type correct?\nNote that for options, the error type must contain a `Unit`."
          )
        case _ => scala.compiletime.error("`.ok()` can only be used within an `either` call.\nIs it present?")
      
  end extension

end HardDrivesTabViewModel





