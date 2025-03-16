package org.itsadigitaltrust.hardwarelogger.services

import org.itsadigitaltrust.hardwarelogger.services.NotificationCentre


import scala.collection.mutable

type NotificationCallback = (key: String, args: Seq[Any]) => Unit

trait NotificationCentre:

  import org.itsadigitaltrust.common.Operators.notIn

  private val notifications: mutable.Map[String, Vector[NotificationCallback]] = mutable.Map()

  def subscribe(key: String)(callback: NotificationCallback): Unit =
    if key notIn notifications then
      notifications(key) = Vector()

    notifications(key) = notifications(key) :+ callback


  def publish(key: String, args: Any*): Unit =
    notifications(key).foreach: callback =>
      callback(key, args)

object SimpleNotificationCentre extends NotificationCentre

