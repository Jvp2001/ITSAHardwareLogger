package org.itsadigitaltrust.hardwarelogger.viewmodels.tabs

import org.itsadigitaltrust.common.optional
import org.itsadigitaltrust.hardwarelogger.delegates.{ProgramMode, TableRowDelegate}
import org.itsadigitaltrust.hardwarelogger.models.HardDriveModel
import org.itsadigitaltrust.hardwarelogger.services.NotificationChannel.Save
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.HardDriveTableRowViewModel
import scalafx.beans.property.StringProperty
import org.itsadigitaltrust.common.?


final class HardDrivesTabViewModel(using itsaID: String) extends TabTableViewModel(HardDriveTableRowViewModel.apply, _.hardDrives) with TableRowDelegate[HardDriveTableRowViewModel]:
  val powerOnTime: StringProperty = StringProperty("0")
  val estimatedLifeTime: StringProperty = StringProperty("0")
  val description: StringProperty = StringProperty("")
  val actionsText: StringProperty = StringProperty("No actions needed.")
  
  
  override def onSelected(selectedRow: Option[HardDriveTableRowViewModel]): Unit =
      selectedRow.foreach: row =>
        powerOnTime.value = row.model.powerOnTime
        estimatedLifeTime.value = row.model.estimatedRemainingLifetime
        description.value = row.model.description
        actionsText.value = row.model.actions

  notificationCentre.subscribe(Save): (key, args) =>
    optional:
      given id:String = args.?.asInstanceOf[String]
//    if !ProgramMode.isInNormalMode then
      databaseService ++= data.map(_.model).toSeq

