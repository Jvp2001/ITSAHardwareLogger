package org.itsadigitaltrust.hardwarelogger.viewmodels

import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.models.HLModel
import org.itsadigitaltrust.hardwarelogger.mvvm.ModelWrapper
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.NotificationName.Save
import org.itsadigitaltrust.hardwarelogger.services.ServicesModule
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{Notifiable, NotificationCentre, NotificationName}


trait ViewModel:
  def setup(): Unit = ()


trait TableRowViewModel[M](model: M)(using itsaID: String) extends ViewModel, ServicesModule, Notifiable[NotificationName]:
  protected val wrapper: ModelWrapper[M] = ModelWrapper(model)
  protected val modeToSaveIn: ProgramMode | "both" = "Normal"


  override def onReceivedNotification(message: Message): Unit =
    if message.name == NotificationName.Save then
      save()
  end onReceivedNotification

  def save(): Unit =
    if ProgramMode.mode == modeToSaveIn || modeToSaveIn == "both" then
      databaseService += model.asInstanceOf[HLModel]
  end save

end TableRowViewModel

    



  
  
