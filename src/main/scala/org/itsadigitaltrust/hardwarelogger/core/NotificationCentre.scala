package org.itsadigitaltrust.hardwarelogger.core

import scala.collection.mutable

type NotificationCallback = (key: String, args: Seq[Any]) => Unit
trait NotificationCentre:
  import Operators.notIn
  private val notifications: mutable.Map[String, List[NotificationCallback]] = mutable.Map[String, List[NotificationCallback]]()

  def subscribe(key: String)(callback: NotificationCallback): Unit =
    if key notIn notifications then
      notifications(key) =  List()

  def publish(key: String, args: Any*): Unit =
    notifications(key).foreach: callback =>
      callback(key, args)

final class SimpleNotificationCentre extends NotificationCentre

