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

  notificationCentre.subscribe(Save): (key, _) =>
    save()

  def save(): Unit =
    if ProgramMode.isInNormalMode then
      val model = wrapper.model match
        case Some(value) => value
        case _ => return
      databaseService += model.asInstanceOf[HLModel]
    
    



  
  
