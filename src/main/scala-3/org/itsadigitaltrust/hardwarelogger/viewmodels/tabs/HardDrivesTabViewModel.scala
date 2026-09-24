package org.itsadigitaltrust.hardwarelogger.viewmodels.tabs

import javafx.scene.control as jfxsc
import javafx.scene.control.TableSelectionModel
import org.itsadigitaltrust.hardwarelogger.delegates.{ProgramMode, TableRowDelegate}
import org.itsadigitaltrust.hardwarelogger.services.{HLDatabaseService, HardwareGrabberService}
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.HardDriveTableRowViewModel
import scalafx.beans.property.{BooleanProperty, ObjectProperty}
import org.itsadigitaltrust.hardwarelogger.core.ui.*
import scalafx.scene.input.MouseButton.Primary
import ox.{Fork, Ox, either, pipe}
import either.ok
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState

import scala.compiletime.summonFrom
import scala.ref.WeakReference
import scala.util.boundary
import scala.util.boundary.break
import javafx.scene.input as jfxsi
import javafx.scene.input.KeyEvent
import org.itsadigitaltrust.common.Operators.{??, |>}
import org.itsadigitaltrust.hardwarelogger.models.HLModel
import org.itsadigitaltrust.hardwarelogger.models.HLModel.HardDriveModel
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.PerformBackendValidationListener
import org.itsadigitaltrust.hardwarelogger.services.validation.*
import org.itsadigitaltrust.hardwarelogger.shared.HardwareLoggerSharedData
import scalafx.application.Platform
import scalafx.scene.control.Alert.AlertType.Information

import scala.reflect.Selectable.reflectiveSelectable


final class HardDrivesTabViewModel(using databaseService: HLDatabaseService)(using HardwareGrabberService)
  extends TabTableViewModel(HardDriveTableRowViewModel.apply, _.drives) with TableRowDelegate[HardDriveTableRowViewModel]:
  private final val unhealthyDriveClass = "unhealthy-drive"


  val editIDColumn = BooleanProperty(false)
  // When its value is changed, it will trigger the table view to edit this cell
  val editingCellProperty: ObjectProperty[Option[(row: Int, column: Int)]] = ObjectProperty(None)
  
  val editableProperty: BooleanProperty = BooleanProperty(false)
  
  var moreInfoDisabledProperty: BooleanProperty = BooleanProperty(false)
  editableProperty <== ProgramMode.modeProperty.isEqualTo("HardDrive")
  
  private var shouldAddWipingRecords = false


  
  
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

  def showExtraInfo(r: HardDriveTableRowViewModel): Unit =
    Dialogs.showHardDriveExtraInfoDialog(r.model)

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

  override def onSelected(selectedRow: Option[HardDriveTableRowViewModel]): Unit =
    moreInfoDisabledProperty.value = selectedRow.isEmpty

  override def onProgramModeChanged(mode: ProgramMode): Either[Error, Unit] =
    editIDColumn() = ProgramMode.isInHardDriveMode
    Right(())

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

  def isCellEditable(row: HardDriveTableRowViewModel | Null): Boolean =
    row != null && editableProperty()

  private def showDrivesNotWipedAlert(notWipedDrives: Seq[HardDriveModel]) =
    boundary:

      var serials =
        if ProgramMode.isInNormalMode then
          notWipedDrives.map(_.serial).mkString(", ")
        else
          notWipedDrives.map(_.itsaID).mkString(", ")
      end serials
      serials = serials.patch(serials.lastIndexOf(", "), " & ", 0)
      serials = if serials.split(", ").length == 2 then serials.replaceFirst(", ", "") else serials
      serials = if serials.trim.startsWith("& ") then serials.replaceFirst("& ", "") else serials

      val word = serials.length match
        case 0 => boundary.break()
        case 1 => "drive"
        case _ => "drives"


      new Alert(Information, s"The $word '$serials' have not been logged. Do you want to log them now?", ButtonType.Yes, ButtonType.No):
        headerText = "No Wiping Records Found"
        showAndWait() match
          case Some(ButtonType.Yes) => shouldAddWipingRecords = true
          case _ => ()
  end showDrivesNotWipedAlert


end HardDrivesTabViewModel





