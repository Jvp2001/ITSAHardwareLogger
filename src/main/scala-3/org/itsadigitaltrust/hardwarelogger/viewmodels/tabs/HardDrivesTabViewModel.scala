package org.itsadigitaltrust.hardwarelogger.viewmodels.tabs

import org.itsadigitaltrust.hardwarelogger.delegates.{ProgramMode, TableRowDelegate}
import org.itsadigitaltrust.hardwarelogger.dialogs.Dialogs
import org.itsadigitaltrust.hardwarelogger.models.HardDriveModel
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.NotificationName.Save
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.HardDriveTableRowViewModel
import org.itsadigitaltrust.common.*

import scalafx.beans.property.{BooleanProperty, StringProperty}
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{Notifiable, NotificationName}

import javafx.scene.control.ButtonType
import scalafx.scene.control.{TableRow, TextInputDialog}
import scalafx.scene.input.{KeyCode, MouseButton}
import scalafx.scene.input.MouseButton.Primary




final class HardDrivesTabViewModel(using itsaID: String) extends TabTableViewModel(HardDriveTableRowViewModel.apply, _.hardDrives) with TableRowDelegate[HardDriveTableRowViewModel]:
  final val unhealthlyDriveClass = "unhealthy-drive"

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
        case "SSD" if rowModel.model.health.toByte < 50 => unhealthlyDriveClass
        case "HDD" | "HHD" if rowModel.model.health.toByte < 100 => unhealthlyDriveClass
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


  override def onReceivedNotification(message: Message): Unit =
    if message.name == NotificationName.Save then
      onSave(message)
    else if message.name == NotificationName.ProgramModeChanged then
      editIDColumn.value = ProgramMode.isInHardDriveMode





  def onSave(message: Message): Unit =
      given id:String = message.userInfo("id").toString
//    if !ProgramMode.isInNormalMode then
      databaseService ++= data.map(_.model).toSeq



