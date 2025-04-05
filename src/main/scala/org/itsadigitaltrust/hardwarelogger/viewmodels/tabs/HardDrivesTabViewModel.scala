package org.itsadigitaltrust.hardwarelogger.viewmodels.tabs

import org.itsadigitaltrust.hardwarelogger.delegates.TableRowDelegate
import org.itsadigitaltrust.hardwarelogger.models.HardDriveModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.HardDriveTableRowViewModel
import scalafx.beans.property.StringProperty

import scala.util.Random

final class HardDrivesTabViewModel extends TabTableViewModel[HardDriveModel, HardDriveTableRowViewModel](HardDriveTableRowViewModel.apply, _.hardDrives) with TableRowDelegate[HardDriveTableRowViewModel]:
  val powerOnTime: StringProperty = StringProperty("0")
  val estimatedLifeTime: StringProperty = StringProperty("0")
  val description: StringProperty = StringProperty("")
  val actionsText: StringProperty = StringProperty("")
  override def onSelected(selectedRow: Option[HardDriveTableRowViewModel]): Unit =
      selectedRow.foreach: row =>
        powerOnTime.value = row.model.powerOnTime
        estimatedLifeTime.value = row.model.estimatedRemainingLifetime
        description.value = row.model.description
        actionsText.value = row.model.actions



