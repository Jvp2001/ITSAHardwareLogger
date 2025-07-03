package org.itsadigitaltrust.hardwarelogger.viewmodels.tabs

import org.itsadigitaltrust.hardwarelogger.delegates.{ProgramMode, TableRowDelegate}
import org.itsadigitaltrust.hardwarelogger.dialogs.Dialogs
import org.itsadigitaltrust.hardwarelogger.models.HardDriveModel
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.NotificationName.Save
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.HardDriveTableRowViewModel

import scalafx.beans.property.{BooleanProperty, StringProperty}
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{Notifiable, NotificationName}

import scalafx.scene.input.MouseButton
import scalafx.scene.input.MouseButton.Primary


final class HardDrivesTabViewModel(using itsaID: String) extends TabTableViewModel(HardDriveTableRowViewModel.apply, _.hardDrives) with TableRowDelegate[HardDriveTableRowViewModel]:
  var moreInfoDisabledProperty: BooleanProperty = BooleanProperty(false)
  val rowDelegate: TableRowDelegate[HardDriveTableRowViewModel] = this
  override def setup(): Unit =
    super.setup()

  def setData(): Unit =
    hardwareGrabberService.hardDrives.map(HardDriveTableRowViewModel.apply).foreach: datum =>
      System.out.println(datum)
      data.add(datum)

  override def onRowDoubleClicked(button: MouseButton, row: Option[HardDriveTableRowViewModel]): Unit =
    button match
      case Primary =>
        row.foreach: r =>
          showExtraInfo(r)

  def showExtraInfo(r: HardDriveTableRowViewModel): Unit =
    Dialogs.showHardDriveExtraInfoDialog(r.model)

  override def onSelected(selectedRow: Option[HardDriveTableRowViewModel]): Unit =
    moreInfoDisabledProperty.value = selectedRow.isEmpty


  override def onReceivedNotification(message: Message): Unit =
    if message.name == NotificationName.Save then
      onSave(message)



  def onSave(message: Message): Unit =
      given id:String = message.userInfo("id").toString
//    if !ProgramMode.isInNormalMode then
      databaseService ++= data.map(_.model).toSeq



