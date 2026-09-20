package org.itsadigitaltrust.hardwarelogger.views.tabs

import org.itsadigitaltrust.common.Operators.{??, |>, ||>}
import org.itsadigitaltrust.hardwarelogger.models.HLModel.BatteryStatusModel
import org.itsadigitaltrust.hardwarelogger.services.HardwareGrabberService
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.BatteryStatusTableRowViewModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.tabs.BatteryStatusTabViewModel
import scalafx.scene.control.TableCell
import org.itsadigitaltrust.hardwarelogger.core.ui.*

import scala.jdk.javaapi.CollectionConverters
class BatteryStatusTabView(using HardwareGrabberService)(using vm: BatteryStatusTabViewModel = new BatteryStatusTabViewModel) extends TabTableView[BatteryStatusTabViewModel#Model, BatteryStatusTabViewModel#RowViewModel, BatteryStatusTabViewModel]:

//  BatteryStatusTableRowViewModel.apply ||>
//    sharedApplicationData.battery ?? Seq() |>
//    CollectionConverters.asJavaCollection |>
//    items.value.setAll

  val chargeColumn = createAndAddColumn("Charge"): cellValue => 
    cellValue.chargeProperty

  val statusColumn = createAndAddColumn("Status")(_.statusProperty)

  val healthColumn = createAndAddColumn("Health")(_.healthProperty)

  

//  healthColumn.cellFactory = (cell, value) =>
//    val health = value.dropWhile(_ == "%").mkString.toDouble
//    if health > 75.0 then
//      cell.style = "-fx-background-color: #00FF00;"
//    else if health > 50.0 then
//      cell.style = "-fx-background-color: #FFFF00;"
//    else
//      cell.style = "-fx-background-color: #FF0000;"
//

