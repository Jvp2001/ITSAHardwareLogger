package org.itsadigitaltrust.hardwarelogger.viewmodels.tabs

import org.itsadigitaltrust.hardwarelogger.delegates.{ProgramMode, TableRowDelegate}
import org.itsadigitaltrust.hardwarelogger.models.HardDriveModel
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.NotificationName.Save
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.HardDriveTableRowViewModel

import scalafx.beans.property.StringProperty


import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{Notifiable, NotificationName}


final class HardDrivesTabViewModel(using itsaID: String) extends TabTableViewModel(HardDriveTableRowViewModel.apply, _.hardDrives) with TableRowDelegate[HardDriveTableRowViewModel]:
  val powerOnTime: StringProperty = StringProperty("0")
  val estimatedLifeTime: StringProperty = StringProperty("0")
  val description: StringProperty = StringProperty("")
  val actionsText: StringProperty = StringProperty("No actions needed.")

  override def setup(): Unit =
    super.setup()

  def setData(): Unit =
    hardwareGrabberService.hardDrives.map(HardDriveTableRowViewModel.apply).foreach: datum =>
      println(datum)
      data.add(datum)

  override def onSelected(selectedRow: Option[HardDriveTableRowViewModel]): Unit =
      selectedRow.foreach: row =>
        powerOnTime.value = row.model.powerOnTime
        estimatedLifeTime.value = row.model.estimatedRemainingLifetime
        description.value = row.model.description
        actionsText.value = row.model.actions

  override def onReceivedNotification(message: Message): Unit =
    if message.name == NotificationName.Save then
      onSave(message)



  def onSave(message: Message): Unit =
      given id:String = message.userInfo("id").toString
//    if !ProgramMode.isInNormalMode then
      databaseService ++= data.map(_.model).toSeq

