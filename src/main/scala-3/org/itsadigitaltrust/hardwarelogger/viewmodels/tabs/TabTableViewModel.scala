package org.itsadigitaltrust.hardwarelogger.viewmodels.tabs

import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode

import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import org.itsadigitaltrust.hardwarelogger.models.{HLModel, HardwareModel}
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{Notifiable, NotificationCentre, NotificationName}
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.NotificationName.Reload
import org.itsadigitaltrust.hardwarelogger.services.{HardwareGrabberService, ServicesModule}
import org.itsadigitaltrust.hardwarelogger.viewmodels.ViewModel

import scalafx.collections.*

import scala.runtime.{AbstractFunction1, AbstractPartialFunction}
import org.itsadigitaltrust.hardwarelogger.services.given
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.TableRowViewModel

import scala.reflect.ClassTag
class TabTableViewModel[M <: HLModel : ClassTag, VM <: TableRowViewModel[M]](rowCtor: M => VM, reloadData: HardwareGrabberService => Seq[M], saveDataMode: ProgramMode = "Normal")(using itsaID: String) extends ViewModel with ServicesModule with Notifiable[NotificationName]:

  type RowViewModel = VM
  val data: ObservableBuffer[VM] = ObservableBuffer()
  notificationCentre.addObserver(this)

  def models: Seq[M] = data.map(_.model).toSeq

  override def setup(): Unit =
    super.setup()

  override def reload(shouldClearData: Boolean = true): Unit =
    if shouldClearData then
      data.clear()
    val newData = reloadData(hardwareGrabberService).map(rowCtor)
    data.addAll(newData)


  override def onReceivedNotification(message: Message): Unit =
    if message.name == NotificationName.Reload then
      reload()
      if data.isEmpty then
        reload(shouldClearData = false)
    else if message.name == NotificationName.Save && (saveDataMode == "Both" || saveDataMode == ProgramMode.mode) then
      if data.isEmpty then
        reload()
        
      logger.info(s"Saving ${models.size} models")
      save()
  end onReceivedNotification

  protected def save(): Unit =
    databaseService ++= models

end TabTableViewModel

