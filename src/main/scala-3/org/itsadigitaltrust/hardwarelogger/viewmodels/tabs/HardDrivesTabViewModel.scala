package org.itsadigitaltrust.hardwarelogger.viewmodels.tabs

import org.itsadigitaltrust.hardwarelogger.delegates.{ProgramMode, TableRowDelegate}
import org.itsadigitaltrust.hardwarelogger.dialogs.Dialogs
import org.itsadigitaltrust.hardwarelogger.models.HardDriveModel
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.NotificationName.Save
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.HardDriveTableRowViewModel
import org.itsadigitaltrust.common.*

import org.itsadigitaltrust.hardwarelogger.services.ApplicationServicesRegistry

import scalafx.beans.property.{BooleanProperty, StringProperty}
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{NotificationName, SaveMessageArgs}
import org.itsadigitaltrust.hardwarelogger.tasks.HardwareLoggerTaskContext

import javafx.scene.control.ButtonType
import scalafx.scene.control.{TableRow, TextInputDialog}
import scalafx.scene.input.{KeyCode, MouseButton}
import scalafx.scene.input.MouseButton.Primary
import sun.jvm.hotspot.runtime.PerfMemory.end

import scala.concurrent.duration.{Duration, DurationInt}




final class HardDrivesTabViewModel extends TabTableViewModel(HardDriveTableRowViewModel.apply, _.hardDrives) with TableRowDelegate[HardDriveTableRowViewModel] with ApplicationServicesRegistry:
  private final val unhealthyDriveClass = "unhealthy-drive"

  val editIDColumn = BooleanProperty(false)

  var moreInfoDisabledProperty: BooleanProperty = BooleanProperty(false)
  val rowDelegate: TableRowDelegate[HardDriveTableRowViewModel] = this
  override def setup(): Unit =
    super.setup()

  def setData(): Unit =
    hardwareGrabberService.hardDrives.map(HardDriveTableRowViewModel.apply).foreach: datum =>
      logger.info(datum.toString)
      data.add(datum)

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

    notificationCentre.addObserver(NotificationName.Save)(onSave)
    notificationCentre.addObserver(NotificationName.ProgramModeChanged): _ =>
      editIDColumn.value = ProgramMode.isInHardDriveMode

  private def onSave(args: SaveMessageArgs): Unit =

    taskBuilder <<- models.map(databaseService.insertOrUpdateModel)
    taskBuilder --> reload(shouldClearData = false)
  end onSave
end HardDrivesTabViewModel





