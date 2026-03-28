package org.itsadigitaltrust.hardwarelogger.services.notificationcentre


import org.itsadigitaltrust.common.Operators.{notIn, |>}
import org.itsadigitaltrust.common.collections.{CaseIterable, Dict}
import org.itsadigitaltrust.common.logging.HWLLoggable

import org.itsadigitaltrust.hardwarelogger.services.{FrontendService, NotificationCentreModule, notificationcentre}

import scala.collection.mutable

trait NotificationCentreService[M] extends HWLLoggable:

  type Observer[T <: this.type#Message] = MessageArgs[T] => Unit
  type Message = M
  /** This is a matched type to return the correct type of the message args. */
  type MessageArgs[T <: this.type#Message]





  def addObserver[MT <: Message, A <: MessageArgs[MT]](message: MT)(observer: A => Unit): Unit

  protected def notify[T <: this.type#Message](message: T, args: MessageArgs[T]): Unit 
  def post[A](message: Message, args: A): Unit


end NotificationCentreService