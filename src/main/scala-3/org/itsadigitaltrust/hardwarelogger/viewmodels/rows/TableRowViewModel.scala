package org.itsadigitaltrust.hardwarelogger.viewmodels.rows

import org.itsadigitaltrust.hardwarelogger.models.HLModel
import org.itsadigitaltrust.hardwarelogger.mvvm.ModelWrapper
import org.itsadigitaltrust.hardwarelogger.services.{ApplicationServicesRegistry, HardwareGrabberService}
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState
import org.itsadigitaltrust.hardwarelogger.viewmodels.{ViewModel, ViewModelWithWrapper}

import scala.reflect.ClassTag

abstract class TableRowViewModel[M <: HLModel : ClassTag](_model: M)(using HardwareGrabberService) extends ViewModelWithWrapper(_model)
