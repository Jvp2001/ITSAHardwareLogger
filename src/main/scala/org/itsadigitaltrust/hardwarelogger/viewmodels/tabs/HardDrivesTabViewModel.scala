package org.itsadigitaltrust.hardwarelogger.viewmodels.tabs

import org.itsadigitaltrust.hardwarelogger.delegates.TableRowDelegate
import org.itsadigitaltrust.hardwarelogger.models.HardDriveModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.HardDriveTableRowViewModel
import scalafx.beans.property.StringProperty

import scala.util.Random

final class HardDrivesTabViewModel extends TabTableViewModel[HardDriveModel, HardDriveTableRowViewModel](HardDriveTableRowViewModel.apply, _.hardDrives) with TableRowDelegate[HardDriveTableRowViewModel]:
  val powerOnTime: StringProperty = StringProperty("0")
  val estimatedLifeTime: StringProperty = StringProperty("0")
  override def onSelected(row: HardDriveTableRowViewModel): Unit =
    powerOnTime.value = Random.between(1, 100).toString
    estimatedLifeTime.value = Random.between(1, 1000).toString
    


