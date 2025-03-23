package org.itsadigitaltrust.hardwarelogger.viewmodels

import scalafx.collections.*
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import org.itsadigitaltrust.hardwarelogger.models.HardwareModel
import org.itsadigitaltrust.hardwarelogger.services.NotificationChannel.Reload
import org.itsadigitaltrust.hardwarelogger.services.{HardwareGrabberService, NotificationCentre, ServicesModule}

import scala.runtime.AbstractPartialFunction
import scala.runtime.AbstractFunction1

class TabTableViewModel[M, VM <: TableRowViewModel[M]](rowCtor: M => VM, reloadData: HardwareGrabberService => Seq[M]) extends ViewModel with ServicesModule:

  import TabTableViewModel.alert

  type RowViewModel = VM
  val data: ObservableBuffer[VM] = ObservableBuffer()

  def reload(): Unit =
    data.clear()

    data.addAll(reloadData(hardwareGrabberService).map(rowCtor))
    if !alert.isShowing then
      alert.showAndWait()


  notificationCentre.subscribe(Reload): (*, _) =>
    reload()


  // object TabTableViewModel:
  //
  // @Bean
  // final class ReloadData[R](f: HardwareGrabberService => R):
  //   def apply(hardwareGrabberService: HardwareGrabberService): R = f(hardwareGrabberService)


end TabTableViewModel


object TabTableViewModel:
  val alert = new Alert(AlertType.INFORMATION, "Loaded!")
