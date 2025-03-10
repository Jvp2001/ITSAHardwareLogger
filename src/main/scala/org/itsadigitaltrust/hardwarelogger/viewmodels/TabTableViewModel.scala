package org.itsadigitaltrust.hardwarelogger.viewmodels

import javafx.collections.{FXCollections, ObservableList}
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import org.itsadigitaltrust.hardwarelogger.core.NotificationCentre
import org.itsadigitaltrust.hardwarelogger.models.HardwareModel
import org.itsadigitaltrust.hardwarelogger.services.HardwareGrabberService

final class TabTableViewModel[M, VM <: TableRowViewModel[M]](hardwareGrabberService: HardwareGrabberService, notificationCentre: NotificationCentre, reloadData: => HardwareGrabberService => Seq[VM]) extends ViewModel:
  val data: ObservableList[VM] = FXCollections.emptyObservableList()

  def reload(): Unit =
    data.clear()

    data.addAll(reloadData(hardwareGrabberService) *)

    val alert = new Alert(AlertType.INFORMATION, "Loaded!")
    alert.showAndWait()


  notificationCentre.subscribe("RELOPAD"): (*, _) =>
    reload()


object TabTableViewModel:
  type ReloadData[E, T <: E] = HardwareGrabberService => List[T]
