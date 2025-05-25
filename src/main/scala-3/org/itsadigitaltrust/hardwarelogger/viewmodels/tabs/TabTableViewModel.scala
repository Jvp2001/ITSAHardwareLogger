package org.itsadigitaltrust.hardwarelogger.viewmodels.tabs

import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import org.itsadigitaltrust.hardwarelogger.models.HardwareModel
import org.itsadigitaltrust.hardwarelogger.services.NotificationChannel.Reload
import org.itsadigitaltrust.hardwarelogger.services.{HardwareGrabberService, NotificationCentre, ServicesModule}
import org.itsadigitaltrust.hardwarelogger.viewmodels.{TableRowViewModel, ViewModel}
import scalafx.collections.*

import scala.runtime.{AbstractFunction1, AbstractPartialFunction}

class TabTableViewModel[M, VM <: TableRowViewModel[M]](rowCtor: M => VM, reloadData: HardwareGrabberService => Seq[M])(using itsaID: String = "") extends ViewModel with ServicesModule:

  type RowViewModel = VM
  val data: ObservableBuffer[VM] = ObservableBuffer()

  def reload(): Unit =
    data.clear()
    data.addAll(reloadData(hardwareGrabberService).map(rowCtor))

  notificationCentre.subscribe(Reload): (*, _) =>
    reload()

end TabTableViewModel

