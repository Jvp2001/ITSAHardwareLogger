package org.itsadigitaltrust.hardwarelogger.services.notificationcentre


import org.itsadigitaltrust.common.Operators.|>
import org.itsadigitaltrust.common.collections.{CaseIterable, Dict}

import org.itsadigitaltrust.hardwarelogger.services.{NotificationCentreModule, notificationcentre}

import scala.collection.mutable

type NotificationUserInfo = Dict

object NotificationUserInfo:
  transparent inline def apply(inline block: => Unit): NotificationUserInfo =
    Dict(block)



type NotificationCallback[Message] = (msg: Message) => Unit

trait NotificationCentre[Name] extends AutoCloseable:
  type Message <: NotificationCentre.Message[Name]
  type Observer = Notifiable[Name]
  type MessageName <: Name


  protected val observers: mutable.Map[Name, mutable.ListBuffer[Observer]] = mutable.Map()


  def addObserverTo(names: MessageName*)(observer: Observer): Unit =
    val msgNames = if names.isEmpty then observers.keys else names
    msgNames.foreach: name =>
      if !observers.contains(name) then
        observers(name) = mutable.ListBuffer()
      observers(name) += observer
    ()

  def addObserver(observer: Observer): Unit



  def post(message: Message): Unit

  def post(name:Name, sender: Option[Any] = None, userInfo: Option[NotificationUserInfo] = None): Unit
  transparent inline def post(name:Name, sender: Option[Any])(inline block: => Unit): Unit =
    post(name, sender, Option(NotificationUserInfo(block)))
//
//  def post[N <: MessageName](name: N, sender:  Option[Any] = None, userInfo: Option[NotificationUserInfo] = None): Unit =
//    val message = messageFactory(name, sender, userInfo)
//    observers.getOrElse(name, mutable.Queue.empty).foreach: item =>
//      item.apply(message.asInstanceOf[item.type#Message])


  override def close(): Unit =
    observers.foreach(_._2.clear())

end NotificationCentre

object NotificationCentre:
  trait Message[MessageName](messageName: MessageName, senderObj: Option[Any] = None, info: NotificationUserInfo = new NotificationUserInfo):
    type Name = MessageName
    val name: Name = messageName
    val sender: Option[Any] = senderObj
    val userInfo: NotificationUserInfo = info

  end Message
end NotificationCentre


trait Notifiable[Name] extends NotificationCentreModule:
  type Message = notificationCentre.type#Message
  val names: Seq[notificationCentre.type#MessageName] = Seq()


  def onReceivedNotification(message: Message): Unit

  def apply(message: Message): Unit = onReceivedNotification(message)
end Notifiable


