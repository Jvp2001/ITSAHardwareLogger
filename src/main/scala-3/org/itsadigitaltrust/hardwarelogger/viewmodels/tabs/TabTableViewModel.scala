package org.itsadigitaltrust.hardwarelogger.viewmodels.tabs

import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import org.itsadigitaltrust.hardwarelogger.models.HardwareModel
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{Notifiable, NotificationCentre, NotificationName}
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.NotificationName.Reload
import org.itsadigitaltrust.hardwarelogger.services.{HardwareGrabberService, ServicesModule}
import org.itsadigitaltrust.hardwarelogger.viewmodels.{TableRowViewModel, ViewModel}

import scalafx.collections.*

import scala.runtime.{AbstractFunction1, AbstractPartialFunction}
import org.itsadigitaltrust.hardwarelogger.services.given
class TabTableViewModel[M, VM <: TableRowViewModel[M]](rowCtor: M => VM, reloadData: HardwareGrabberService => Seq[M])(using itsaID: String) extends ViewModel with ServicesModule with Notifiable[NotificationName]:

  type RowViewModel = VM
  val data: ObservableBuffer[VM] = ObservableBuffer()

  notificationCentre.addObserver(this)

  def reload(): Unit =
    data.clear()
    data.addAll(reloadData(hardwareGrabberService).map(rowCtor))

  reload()

  override def onReceivedNotification(message: Message): Unit =
    if message.name == NotificationName.Reload then
      reload()

end TabTableViewModel

