package org.itsadigitaltrust.hardwarelogger.viewmodels

import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.models.HLModel
import org.itsadigitaltrust.hardwarelogger.mvvm.ModelWrapper
import org.itsadigitaltrust.hardwarelogger.services.NotificationChannel.Save
import org.itsadigitaltrust.hardwarelogger.services.ServicesModule


trait ViewModel:
  def setup(): Unit = ()


trait TableRowViewModel[M](model: M) extends ViewModel with ServicesModule:
  protected val wrapper: ModelWrapper[M] = ModelWrapper(model)
  protected val modeToSaveIn: ProgramMode | "both" = "Normal"
  notificationCentre.subscribe(Save): (key, _) =>
    save()

  def save(): Unit =
    if ProgramMode.mode == modeToSaveIn || modeToSaveIn == "both" then
      databaseService += model.asInstanceOf[HLModel]
  end save

end TableRowViewModel

    



  
  
