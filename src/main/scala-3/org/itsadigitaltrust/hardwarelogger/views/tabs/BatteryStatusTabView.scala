package org.itsadigitaltrust.hardwarelogger.views.tabs

import atlantafx.base.theme.Styles
import javafx.scene.{control, control as jfxsc}
import javafx.util.Callback
import org.itsadigitaltrust.common.Operators.{??, |>, ||>}
import org.itsadigitaltrust.hardwarelogger.contextbasedemnubar.MenuManagerService
import org.itsadigitaltrust.hardwarelogger.models.HLModel.BatteryStatusModel
import org.itsadigitaltrust.hardwarelogger.services.HardwareGrabberService
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.BatteryStatusTableRowViewModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.tabs.BatteryStatusTabViewModel
import scalafx.scene.control.TableCell
import org.itsadigitaltrust.hardwarelogger.core.ui.*
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState

import scala.jdk.javaapi.CollectionConverters

class BatteryStatusTabView(using BatteryStatusTabViewModel) extends TabTableView[BatteryStatusTabViewModel#Model, BatteryStatusTabViewModel#RowViewModel, BatteryStatusTabViewModel]:

  //  BatteryStatusTableRowViewModel.apply ||>
  //    sharedApplicationData.battery ?? Seq() |>
  //    CollectionConverters.asJavaCollection |>
  //    items.value.setAll


  class PercentageTableCell extends jfxsc.TableCell[BatteryStatusTabViewModel#RowViewModel, Double]:

    override def updateItem(value: Double, empty: Boolean): Unit =
      super.updateItem(value, empty)
      val healthStateStyles = Seq(Styles.DANGER, Styles.WARNING, Styles.SUCCESS)
      styleClass.removeAll(healthStateStyles*)

      if empty then
        setText(null)
        setGraphic(null)
        setStyle("") // Clear style to prevent bleeding on recycled cells
      else
        val healthVal = value.doubleValue()

        formatText(healthVal)
        setText(f"$healthVal")

    end updateItem
    protected def formatText(value: Double): Unit = ()
  end PercentageTableCell



  val chargeColumn = createAndAddColumn("Charge"): cellValue =>
    cellValue.chargeProperty

  chargeColumn.delegate.setCellFactory((param: control.TableColumn[BatteryStatusTabViewModel#RowViewModel, Double]) =>
      new PercentageTableCell:
        override protected def formatText(value: Double): Unit =
          if value <= 10.0 then
            styleClass += Styles.DANGER
            setStyle("-fx-text-fill: -color-danger-fg;")
          else
            styleClass -= Styles.DANGER
            setStyle("-fx-text-fill: -color-fg-default;")
    )
  val statusColumn = createAndAddColumn("Status")(_.statusProperty)

  val healthColumn = createAndAddColumn("Health")(_.healthProperty)
  
  import scalafx.scene.control.TableCell
  import javafx.scene.{control => jfxsc}

  healthColumn.delegate.setCellFactory((param: control.TableColumn[BatteryStatusTabViewModel#RowViewModel, Double]) =>
   new PercentageTableCell:
     override protected def formatText(healthVal: Double): Unit =
       if healthVal > 75.0 then
         styleClass += Styles.SUCCESS
         setStyle("-fx-text-fill: -color-success-fg;") // Explicitly hooks into AtlantaFX's theme palette
       else if healthVal > 50.0 then
         styleClass += Styles.WARNING
         setStyle("-fx-text-fill: -color-warning-fg;")
       else
         styleClass += Styles.DANGER
         setStyle("-fx-text-fill: -color-danger-fg;")
  )

//  healthColumn.cellFactory = (cell, value) =>
//    val health = value.dropWhile(_ == "%").mkString.toDouble
//    if health > 75.0 then
//      cell.style = "-fx-background-color: #00FF00;"
//    else if health > 50.0 then
//      cell.style = "-fx-background-color: #FFFF00;"
//    else
//      cell.style = "-fx-background-color: #FF0000;"
//

