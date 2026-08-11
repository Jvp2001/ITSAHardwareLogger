package org.itsadigitaltrust.hardwarelogger.services.notificationcentre

import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.services.HardwareGrabberService
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState
import ox.either
import ox.either.{fail, ok}
import scalafx.beans.property.{BufferProperty, ObjectProperty, StringProperty}

import scala.collection.mutable
import scala.compiletime.deferred
import scala.util.NotGiven
import scala.language.experimental.relaxedLambdaSyntax

trait ListenerExecutionConditionContext:
  def canExecute: Boolean
  
object ListenerExecutionConditionContext:
  given defaultContext: ListenerExecutionConditionContext = new ListenerExecutionConditionContext:
    override def canExecute: Boolean = true

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
object NotificationCentre:
  type Error = HardwareGrabberService#Error
  private type ListenerReturnType = Either[Error, Unit]
  private type NoArgNotificationDispatchCallback = () => ListenerReturnType
  private type ApplicationState = HardwareLoggerApplicationState
  private type ReloadCallback = () => ApplicationState ?=> ListenerReturnType


  final class Dispatcher[Callback]  extends mutable.HashMap[String, Callback]
  private val reloadDispatcher = Dispatcher[ReloadCallback]()
  private val continueWithDuplicateDrivesDispatcher = Dispatcher[Seq[String] => ListenerReturnType]()
  private val programModeChangedDispatcher = Dispatcher[ProgramMode => ListenerReturnType]()
  private val saveDBDispatcher = Dispatcher[NoArgNotificationDispatchCallback]()
  private val duplicateIDWasFoundDispatcher= StringProperty("")
  private val onConnectedSuccessfullyDispatcher = Dispatcher[NoArgNotificationDispatchCallback]()
  final case class ListenerResult(name: String, result: ListenerReturnType)

  def continueWithDuplicateDrives(drives: Seq[String]): Unit = continueWithDuplicateDrivesDispatcher.values.foreach(_.apply(drives))
  def programModeChanged(mode: ProgramMode): ListenerReturnType =
    
    ox.either:
      for callback <- programModeChangedDispatcher.values do
        callback(mode).ok()  
      
  def saveDB(): Unit = saveDBDispatcher.values.foreach(_.apply())
  def duplicateIDWasFound(id: String): Unit = duplicateIDWasFoundDispatcher() = id
 
  
  
  private def createConditionalExecutionFunc(using executionConditionContext: ListenerExecutionConditionContext)(f: () => ListenerReturnType): () => ListenerReturnType =
    () => if executionConditionContext.canExecute then f() else Right(())

  private def createConditionalExecutionFunc[A](using executionConditionContext: ListenerExecutionConditionContext)(f: A => ListenerReturnType)(defaultCallback: A => ListenerReturnType = (_: A) => Right(())): A => ListenerReturnType =
    a => if executionConditionContext.canExecute then f(a) else defaultCallback(a)

  private def createConditionalExecutionFunc[A](using executionConditionContext: ListenerExecutionConditionContext)(f: A => ListenerReturnType): A => ListenerReturnType =
    a => if executionConditionContext.canExecute then f(a) else  Right(())



  def programModeChangeNotificationListeners: Dispatcher[ProgramMode => ListenerReturnType] = programModeChangedDispatcher
  
  def onConnectedSuccessfullyNotificationListeners: Dispatcher[NoArgNotificationDispatchCallback] = onConnectedSuccessfullyDispatcher
  
  def reloadListeners: Dispatcher[ReloadCallback] = reloadDispatcher

  def addProgramModeListener(name: String)(f: ProgramMode => ListenerReturnType)(using executionConditionContext: ListenerExecutionConditionContext): Unit =
    programModeChangedDispatcher += name -> createConditionalExecutionFunc(f)
  def addDuplicateIDListener(f: String => ListenerReturnType)(using executionConditionContext: ListenerExecutionConditionContext): Unit = duplicateIDWasFoundDispatcher.onChange: (op, old, id) =>
    createConditionalExecutionFunc[String](f)(id)




  def addSaveDBListener(name: String)(f: NoArgNotificationDispatchCallback)(using executionConditionContext: ListenerExecutionConditionContext): Unit =
    saveDBDispatcher += name -> createConditionalExecutionFunc(f)
  
  def addOnConnectedSuccessfullyListener(name: String)(f: NoArgNotificationDispatchCallback)(using executionConditionContext: ListenerExecutionConditionContext): Unit =
    onConnectedSuccessfullyDispatcher += name -> createConditionalExecutionFunc(f)

  def addContinueWithDuplicateDrivesListener(name: String)(f: Seq[String] => ListenerReturnType)(using executionConditionContext: ListenerExecutionConditionContext): Unit =
    continueWithDuplicateDrivesDispatcher += name -> createConditionalExecutionFunc(f)


  def addReloadListener(name: String)(f: ReloadCallback)(using executionConditionContext: ListenerExecutionConditionContext): Unit =
    val default = () => (_: ApplicationState) ?=> Right(())
    reloadDispatcher += name -> (
      if executionConditionContext.canExecute then
        f
      else
        default
    )
    
  def reload()(using applicationState: HardwareLoggerApplicationState): Unit =
    reloadDispatcher.foreach(_._2())

end NotificationCentre

sealed trait NotificationCentreListener:
  type Error = HardwareGrabberService#Error
  
  lazy val identifier: String
end NotificationCentreListener

trait ReloadNotificationListener extends NotificationCentreListener:
  NotificationCentre.addReloadListener(identifier)(reload)
  protected def reload()(using hardwareLoggerApplicationState: HardwareLoggerApplicationState): Either[Error, Unit] = Right(())

trait SaveNotificationListener extends NotificationCentreListener:
  NotificationCentre.addSaveDBListener(identifier)(save)
  protected def save(): Either[Error, Unit] = Right(())

trait ProgramModeChangeNotificationListener extends NotificationCentreListener:
  NotificationCentre.addProgramModeListener(identifier)(programModeChanged)

  /**
   * 
   * @note       Any code related to reloading data does not need to be called in here as the reload worker method will handle this. 
   *             See [[tasks.CommonHardwareLoggerTasksAndWorkers#performReloadWorker]] for how the reload worker logic works.
   * @param mode The new [[delegates.ProgramMode]]
   * @return
   */
  protected  def programModeChanged(mode: ProgramMode): Either[Error, Unit] = Right(())

trait ContinueWithDuplicateDriveNotificationListener extends NotificationCentreListener:
  NotificationCentre.addContinueWithDuplicateDrivesListener(identifier)(continueWithDuplicateDrives)
  protected def continueWithDuplicateDrives(drives: Seq[String]): Either[Error, Unit] = Right(())
  
trait DuplicateIDWasFoundNotificationListener extends NotificationCentreListener:
  NotificationCentre.addDuplicateIDListener(duplicateIDWasFound)
  protected def duplicateIDWasFound(itsaID: String): Either[Error, Unit] = Right(())
  
trait ConnectedSuccessfullyNotificationListener extends NotificationCentreListener:
  NotificationCentre.addOnConnectedSuccessfullyListener(identifier)(onConnectedToDBSuccessfully)
  protected def onConnectedToDBSuccessfully(): Either[Error, Unit] = Right(())
  
trait ConnectedSuccessfullyAndReloadListener extends ConnectedSuccessfullyNotificationListener, ReloadNotificationListener

  
  
  

