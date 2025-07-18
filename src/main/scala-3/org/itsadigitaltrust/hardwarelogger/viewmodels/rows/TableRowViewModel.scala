package org.itsadigitaltrust.hardwarelogger.viewmodels.rows

import org.itsadigitaltrust.hardwarelogger.models.HLModel
import org.itsadigitaltrust.hardwarelogger.mvvm.ModelWrapper
import org.itsadigitaltrust.hardwarelogger.services.ServicesModule
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{Notifiable, NotificationName}
import org.itsadigitaltrust.hardwarelogger.viewmodels.ViewModel

import scala.reflect.ClassTag

trait TableRowViewModel[M <: HLModel : ClassTag](_model: M)(using itsaID: String) extends ViewModel, ServicesModule, Notifiable[NotificationName]:
  protected val wrapper: ModelWrapper[M] = ModelWrapper(_model)
  final val model: M = _model

  notificationCentre.addObserver(this)

  override def onReceivedNotification(message: Message): Unit = ()



end TableRowViewModel
