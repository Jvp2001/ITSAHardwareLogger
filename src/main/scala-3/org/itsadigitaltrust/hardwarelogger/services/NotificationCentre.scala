package org.itsadigitaltrust.hardwarelogger.services

import org.itsadigitaltrust.hardwarelogger.services.NotificationCentre


import scala.collection.mutable

type NotificationCallback[ME] = (key: ME, args: Seq[Any]) => Unit

trait NotificationCentre[ME]:
  
  import org.itsadigitaltrust.common.Operators.notIn

  private val notifications: mutable.Map[ME, Vector[NotificationCallback[ME]]] = mutable.Map()

  def subscribe(key: ME)(callback: NotificationCallback[ME]): Unit =
    if key notIn notifications then
      notifications(key) = Vector()

    notifications(key) = notifications(key) :+ callback


  def publish(key: ME, args: Any*): Unit =
    if key notIn notifications then
      notifications(key) = Vector()

    notifications(key).foreach: callback =>
      callback(key, args)

enum NotificationChannel:
  case Reload
  case Save // This channel is used to push all the loaded data to the database.
  case DBError(msg: String)
  
  case DBSuccess
  case FoundDuplicateRowsWithID
  case MarkRowsWithIDAsError
  case ShowDuplicateDriveWarning
  case ContinueWithDuplicateDrive
  case ProgramModeChanged
  
object SimpleNotificationCentre extends NotificationCentre[NotificationChannel]

