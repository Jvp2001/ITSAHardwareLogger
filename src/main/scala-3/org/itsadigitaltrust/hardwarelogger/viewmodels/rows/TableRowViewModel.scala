package org.itsadigitaltrust.hardwarelogger.viewmodels.rows

import org.itsadigitaltrust.hardwarelogger.models.HLModel
import org.itsadigitaltrust.hardwarelogger.mvvm.ModelWrapper
import org.itsadigitaltrust.hardwarelogger.services.ApplicationServicesRegistry
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.NotificationName
import org.itsadigitaltrust.hardwarelogger.viewmodels.ViewModel

import scala.reflect.ClassTag

trait TableRowViewModel[M <: HLModel : ClassTag](_model: M) extends ViewModel, ApplicationServicesRegistry:
  protected val wrapper: ModelWrapper[M] = ModelWrapper(_model)
  final val model: M = _model





end TableRowViewModel
