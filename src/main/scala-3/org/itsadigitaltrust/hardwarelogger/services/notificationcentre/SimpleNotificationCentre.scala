package org.itsadigitaltrust.hardwarelogger.services.notificationcentre

import org.itsadigitaltrust.common.Operators.or
import org.itsadigitaltrust.common.collections.Dict

import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.services.HLDatabaseServiceException
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.NotificationName.{DBError, ShowDuplicateDriveWarning}

import scala.collection.mutable
import scala.language.reflectiveCalls
import scala.reflect.Selectable.reflectiveSelectable

enum NotificationName:
  case Reload  extends NotificationName
  case Save  extends NotificationName// This channel is used to push all the loaded data to the database.

  case DBError extends NotificationName
  case HDRowSelectionChange extends NotificationName
  case DBSuccess extends NotificationName
  case FoundDuplicateRowsWithID extends NotificationName
  case ShowDuplicateDriveWarning extends NotificationName
  case ContinueWithDuplicateDrive extends NotificationName
  case ProgramModeChanged extends NotificationName
end NotificationName


sealed trait HLMessageArgs
type NotificationUserInfo = Dict & HLMessageArgs

final case class DBErrorMessageArgs(error: HLDatabaseServiceException) extends HLMessageArgs
final case class DuplicateDrivesErrorArgs(duplicateRows: Seq[String]) extends HLMessageArgs
final case class ProgramModeChangedArgs(oldMode: ProgramMode, newMode: ProgramMode) extends HLMessageArgs
final case class ReloadMessageArgs() extends HLMessageArgs
final case class SaveMessageArgs() extends HLMessageArgs
final case class DBSuccessArgs() extends HLMessageArgs
case class ContinueWithDuplicateDriveArgs(duplicateDrives: Seq[String]) extends HLMessageArgs
private[services] type HardwareNotificationArgType[M <: NotificationName] = M match
  case NotificationName.DBError.type => DBErrorMessageArgs.type
  case  NotificationName.Reload.type => ReloadMessageArgs.type
  case  NotificationName.ProgramModeChanged.type => ProgramModeChangedArgs.type
  case  NotificationName.ShowDuplicateDriveWarning.type => DuplicateDrivesErrorArgs.type
  case NotificationName.HDRowSelectionChange.type => Unit.type
  case NotificationName.DBSuccess.type => DBSuccessArgs.type
  case NotificationName.FoundDuplicateRowsWithID.type => Unit.type
  case NotificationName.ContinueWithDuplicateDrive.type => ContinueWithDuplicateDriveArgs.type
  case NotificationName.Save.type => SaveMessageArgs.type
private type HardwareLoggerNotificationMessageContainer = Dict:
  val reload:  mutable.Buffer[ReloadMessageArgs => Unit]
  val dbError:  mutable.Buffer[DBErrorMessageArgs => Unit]
  val programModeChanged:  mutable.Buffer[ProgramModeChangedArgs => Unit]
  val showDuplicateDriveWarning:  mutable.Buffer[DuplicateDrivesErrorArgs => Unit]
  val hdRowSelectionChange:  mutable.Buffer[Unit => Unit]
  val dbSuccess:  mutable.Buffer[DBSuccessArgs => Unit]
  val foundDuplicateRowsWithID:  mutable.Buffer[Unit => Unit]
  val continueWithDuplicateDrive:  mutable.Buffer[ContinueWithDuplicateDriveArgs => Unit]
  val save:  mutable.Buffer[SaveMessageArgs => Unit]

object HardwareLoggerNotificationMessageContainer:
  def apply(): HardwareLoggerNotificationMessageContainer = Dict:
    val reload = mutable.Buffer[ ReloadMessageArgs => Unit]()
    val dbError = mutable.Buffer[ DBErrorMessageArgs => Unit]()
    val programModeChanged = mutable.Buffer[ ProgramModeChangedArgs => Unit]()
    val showDuplicateDriveWarning = mutable.Buffer[ DuplicateDrivesErrorArgs => Unit]()
    val hdRowSelectionChange = mutable.Buffer[ Unit => Unit]()
    val dbSuccess = mutable.Buffer[ DBSuccessArgs => Unit]()
    val foundDuplicateRowsWithID = mutable.Buffer[ Unit => Unit]()
    val continueWithDuplicateDrive = mutable.Buffer[ ContinueWithDuplicateDriveArgs => Unit]()
    val save = mutable.Buffer[ SaveMessageArgs => Unit]()

  .asInstanceOf[HardwareLoggerNotificationMessageContainer]
  end apply

object HardwareLoggerNotificationCentre extends NotificationCentreService[NotificationName]:
  override type Message = NotificationName
  override type MessageArgs[T <: Message] = HardwareNotificationArgType[T]
  protected val messages: HardwareLoggerNotificationMessageContainer = HardwareLoggerNotificationMessageContainer()

  private inline def getMessages[M <: Message](message: M): mutable.Buffer[MessageArgs[M] => Unit] =
    val buffer = message match
      case NotificationName.Reload => messages.reload
      case NotificationName.Save => messages.save
      case NotificationName.DBError => messages.dbError
      case NotificationName.ProgramModeChanged => messages.programModeChanged
      case NotificationName.DBSuccess => messages.dbSuccess
      case NotificationName.ContinueWithDuplicateDrive => messages.continueWithDuplicateDrive
      case NotificationName.HDRowSelectionChange => messages.hdRowSelectionChange
      case NotificationName.FoundDuplicateRowsWithID => messages.foundDuplicateRowsWithID
      case NotificationName.ShowDuplicateDriveWarning => messages.showDuplicateDriveWarning
    buffer.asInstanceOf[mutable.Buffer[MessageArgs[M] => Unit]]


  override def addObserver[MT <: Message, A <: HardwareNotificationArgType[MT]](message: MT)(observer: A => Unit): Unit =
    getMessages(message).asInstanceOf[mutable.Buffer[observer.type]] += observer

  override protected def notify[T <: Message](message: T, args: HardwareNotificationArgType[T]): Unit =
    getMessages(message).asInstanceOf[mutable.Buffer[args.type => Unit]].foreach(_.apply(args))

  override def post[A](message: Message, args: A): Unit =
    getMessages(message).asInstanceOf[mutable.Buffer[args.type => Unit]].foreach(_.apply(args))
end HardwareLoggerNotificationCentre

  
export HardwareLoggerNotificationCentre.*