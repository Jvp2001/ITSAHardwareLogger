package org.itsadigitaltrust.hardwarelogger.services.notificationcentre

import org.itsadigitaltrust.common.Operators.??

import scala.collection.mutable

enum NotificationName:
  case Reload
  case Save // This channel is used to push all the loaded data to the database.

  case DBSuccess
  case FoundDuplicateRowsWithID
  case ShowDuplicateDriveWarning
  case ContinueWithDuplicateDrive
  case ProgramModeChanged
end NotificationName


object SimpleNotificationCentre extends NotificationCentre[NotificationName]:
  override type MessageName = NotificationName
  private final case class SimpleMessage(messageName: NotificationName, override val sender: Option[Any], info: NotificationUserInfo = new NotificationUserInfo) extends NotificationCentre.Message(messageName, sender, info)


  override def post(message: SimpleNotificationCentre.Message): Unit =
    observers.getOrElse(message.name, mutable.ListBuffer.empty).foreach: item =>
      item(message.asInstanceOf[item.type#Message])


  override def post(name: NotificationName, sender: Option[Any], userInfo: Option[NotificationUserInfo]): Unit =
    val message = SimpleMessage(name, sender, userInfo ?? new NotificationUserInfo)
    observers(name).foreach: item =>
      item(message.asInstanceOf[item.type#Message])

  override def addObserver(observer: Observer): Unit =
    val seq = NotificationName.values.toSeq
    addObserverTo(seq*)(observer)


export SimpleNotificationCentre.*