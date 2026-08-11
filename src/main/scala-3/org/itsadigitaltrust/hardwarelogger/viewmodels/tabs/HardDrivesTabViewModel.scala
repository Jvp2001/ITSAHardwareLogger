package org.itsadigitaltrust.hardwarelogger.viewmodels.tabs

import javafx.scene.control.ButtonType
import org.itsadigitaltrust.hardwarelogger.delegates.{ProgramMode, TableRowDelegate}
import org.itsadigitaltrust.hardwarelogger.dialogs.Dialogs
import org.itsadigitaltrust.hardwarelogger.services.{HLDatabaseService, HardwareGrabberService}
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.HardDriveTableRowViewModel
import scalafx.beans.property.BooleanProperty
import scalafx.scene.control.{TableRow, TextInputDialog}
import scalafx.scene.input.MouseButton
import scalafx.scene.input.MouseButton.Primary
import ox.either
import either.ok
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState

import scala.compiletime.summonFrom
import scala.util.boundary
import scala.util.boundary.break





final class HardDrivesTabViewModel(using databaseService: HLDatabaseService)(using HardwareGrabberService, HardwareLoggerApplicationState) extends TabTableViewModel(HardDriveTableRowViewModel.apply, _.hardDrives) with TableRowDelegate[HardDriveTableRowViewModel]:
  private final val unhealthyDriveClass = "unhealthy-drive"

  val editIDColumn = BooleanProperty(false)

  var moreInfoDisabledProperty: BooleanProperty = BooleanProperty(false)
  val rowDelegate: TableRowDelegate[HardDriveTableRowViewModel] = this

  override def onUpdateItem(row: Option[HardDriveTableRowViewModel], tableRow: TableRow[HardDriveTableRowViewModel]): Unit =
    tableRow.styleClass += row.map: rowModel =>
      rowModel.model.`type` match
        case "SSD" if rowModel.model.health.toByte < 50 => unhealthyDriveClass
        case "HDD" | "HHD" if rowModel.model.health.toByte < 100 => unhealthyDriveClass
        case _ => ""
    .get
  end onUpdateItem




  override def onRowDoubleClicked(button: MouseButton, row: Option[HardDriveTableRowViewModel]): Unit =
    button match
      case Primary =>
        row.foreach: r =>
          if ProgramMode.isInNormalMode then
            showExtraInfo(r)
          else
            new TextInputDialog(r.idProperty.value):
              headerText = "Change ID"
              dialogPane.delegate.get().lookupButton(ButtonType.OK).disableProperty().bind(delegate.getEditor.textProperty().isEmpty)
            .showAndWait() match
              case Some(value) => r.model.itsaID = value
              case None => ()


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





