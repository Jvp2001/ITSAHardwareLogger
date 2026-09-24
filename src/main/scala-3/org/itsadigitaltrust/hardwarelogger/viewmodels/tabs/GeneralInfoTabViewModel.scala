package org.itsadigitaltrust.hardwarelogger.viewmodels.tabs

import org.itsadigitaltrust.hardwarelogger.models.HLModel.GeneralInfoModel
import org.itsadigitaltrust.hardwarelogger.services.HardwareGrabberService
import org.itsadigitaltrust.hardwarelogger.services.database.HLDatabaseService
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{ListenerExecutionConditionContext, PerformBackendValidationListener}
import org.itsadigitaltrust.hardwarelogger.ui.derivation.annotations.*
import org.itsadigitaltrust.hardwarelogger.viewmodels.{ViewModel, ViewModelWithWrapper}
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.GeneralInfoTableRowViewModel
import scalafx.beans.property.*

case class GeneralInfoTabViewModel(_model: GeneralInfoModel) extends ViewModelWithWrapper(_model):
  @Readonly
  val computerID: StringProperty = wrapper.field("versionID", _.versionID, "")(StringProperty.apply)
  @Readonly
  val description: StringProperty = wrapper.field("description", _.description, "")(StringProperty.apply)
  @Readonly
  @DisplayName("Model")
  val deviceModel: StringProperty = wrapper.field("model", _.model, "")(StringProperty.apply)
  @Readonly
  val batteryHealth: DoubleProperty = wrapper.field[Double, DoubleProperty, DoubleProperty]("battery", _.batteryHealth.toDouble, 0.0)(DoubleProperty.apply)
  @Readonly
  val amountOfMemory: StringProperty = wrapper.field("amountOfMemory", _.amountOfMemory.toString + "GB", "0GB")(StringProperty.apply)
  @Readonly
  val amountOfStorage: StringProperty = wrapper.field("amountOfStorage", _.amountOfStorage.toString + "GB", "0GB")(StringProperty.apply)
end GeneralInfoTabViewModel


