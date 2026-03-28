package org.itsadigitaltrust.hardwarelogger.viewmodels.tabs

import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.models.HLModel.HardwareModel

import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import org.itsadigitaltrust.hardwarelogger.models.HLModel
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{NotificationCentreService, NotificationName}
import org.itsadigitaltrust.hardwarelogger.services.{ApplicationServicesRegistry, HardwareGrabberService, OshiHardwareGrabberApplicationService, given}
import org.itsadigitaltrust.hardwarelogger.tasks.SimpleWaitHardwareLoggerGroupTaskGroupBuilder
import org.itsadigitaltrust.hardwarelogger.viewmodels.ViewModel

import scalafx.collections.*

import scala.runtime.{AbstractFunction1, AbstractPartialFunction}
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.TableRowViewModel

import scalafx.beans.property.BooleanProperty

import scala.reflect.ClassTag
class TabTableViewModel[M <: HLModel : ClassTag, VM <: TableRowViewModel[M]](rowCtor: M => VM, reloadData: => HardwareGrabberService => Seq[M], saveDataMode: ProgramMode = "Normal")(using SimpleWaitHardwareLoggerGroupTaskGroupBuilder[Unit]) extends ViewModel with ApplicationServicesRegistry:
  given taskBuilder: SimpleWaitHardwareLoggerGroupTaskGroupBuilder[Unit] = summon[SimpleWaitHardwareLoggerGroupTaskGroupBuilder[Unit]]

  type RowViewModel = VM
  val data: ObservableBuffer[VM] = ObservableBuffer()
  data.onChange: (source, change) =>
    if change.isEmpty then
      reload()
  notificationCentre.addObserver(NotificationName.Reload): _ =>
    reload()

  notificationCentre.addObserver(NotificationName.Save): args =>
    if saveDataMode == "Both" || saveDataMode == ProgramMode.mode then
      logger.info(s"Saving ${models.size} models")
      taskBuilder <-- save()

  val editable: BooleanProperty = BooleanProperty(false)
  editable.bind(ProgramMode.isHardDriveMode)
  def models: Seq[M] = data.map(_.model).toSeq

  override def setup(): Unit =
    super.setup()

  def scheduleReload(shouldClearData: Boolean = true): Unit =
    taskBuilder --> reload(shouldClearData)


  override def reload(shouldClearData: Boolean = true): Unit =
    given hardwareGrabberService: HardwareGrabberService = summon[HardwareGrabberService]
    if shouldClearData then
      data.clear()
    data ++= reloadData(hardwareGrabberService).map(rowCtor)

  
  protected def save(): Unit =
    ()

end TabTableViewModel

