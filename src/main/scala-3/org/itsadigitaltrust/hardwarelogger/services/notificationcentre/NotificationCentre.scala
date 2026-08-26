package org.itsadigitaltrust.hardwarelogger.services.notificationcentre

import org.itsadigitaltrust.common.Operators.??
import org.itsadigitaltrust.common.macros.NameOf
import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.models.HLModel
import org.itsadigitaltrust.hardwarelogger.services.validation.GlobalValidationContext
import org.itsadigitaltrust.hardwarelogger.services.{HLDatabaseService, HardwareGrabberService}
import org.itsadigitaltrust.hardwarelogger.shared.HardwareLoggerSharedData
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState
import ox.{Ox, either}
import ox.either.{fail, ok}
import scalafx.beans.property.{BufferProperty, ObjectProperty, StringProperty}

import scala.collection.mutable
import scala.compiletime.deferred
import scala.util.NotGiven
import scala.language.experimental.relaxedLambdaSyntax
import scala.reflect.ClassTag

trait ListenerExecutionConditionContext:
  def canExecute: Boolean
  
object ListenerExecutionConditionContext:
  given defaultContext: ListenerExecutionConditionContext = new ListenerExecutionConditionContext:
    override def canExecute: Boolean = true

enum NotificationName:
  case FetchData extends NotificationName
  case GetFetchedData extends NotificationName
  case UpdateSharedData extends NotificationName
  case TabSavePreparation extends NotificationName
  case Reload  extends NotificationName
  case Save  extends NotificationName// This channel is used to push all the loaded data to the database.
  case ItsaIDChanged extends NotificationName
  case DBError extends NotificationName
  case HDRowSelectionChange extends NotificationName
  case DBSuccess extends NotificationName
  case FoundDuplicateRowsWithID extends NotificationName
  case ShowDuplicateDriveWarning extends NotificationName
  case ContinueWithDuplicateDrive extends NotificationName
  case ProgramModeChanged extends NotificationName
end NotificationName

trait ForkBasedEvent(using context: GlobalValidationContext[HLModel]):
  protected val runningForks = mutable.ListBuffer[ox.Fork[Unit]]()

  /**
   * Tabs spawn a virtual thread fork and register it directly into the event buffer.
   */
  def registerTabValidation(block: => Unit): Unit = ox.supervised:
    val tabFork = ox.fork(block)
    runningForks += tabFork


  def getForks(using ox.Ox): List[ox.Fork[Unit]] =
    runningForks.toList
end ForkBasedEvent

case class TabPreparationEvent()(using context: GlobalValidationContext[HLModel])(using ox.Ox) extends ForkBasedEvent

final case class BackendValidationEvent()(using context: GlobalValidationContext[HLModel])(using ox.Ox) extends ForkBasedEvent

object NotificationCentre:
  type Error = Throwable //HardwareGrabberService#Error
  private type ListenerReturnType[E <: Error] = Either[E, Unit]
  private type NoArgNotificationDispatchCallback[E <: Error] = () => ListenerReturnType[E]
  private type ApplicationState = HardwareLoggerSharedData
  private type ReloadCallback[E <: Error] = () => ApplicationState ?=> ListenerReturnType[E]


  final class Dispatcher[C]  extends mutable.HashMap[String, C]:
    type Callback = C

  private var fetchDataListener: Option[NoArgNotificationDispatchCallback[HardwareGrabberService#Error]] = None
  private var fetchGlobalValidationContext: Option[() => GlobalValidationContext[? <: HLModel]] = None
  
  private var getFetchedDataListener: Option[() => Option[HardwareLoggerSharedData]] = None
  private val reloadDispatcher = Dispatcher[ReloadCallback[Throwable]]()
  private val programModeChangedDispatcher = Dispatcher[ProgramMode => ox.Ox ?=> ListenerReturnType[Throwable]]()
  private val saveDBDispatcher = Dispatcher[HardwareLoggerSharedData => ox.Ox ?=> ListenerReturnType[Throwable]]()
  private val duplicateIDWasFoundDispatcher= StringProperty("")
  private val onConnectedSuccessfullyDispatcher = Dispatcher[NoArgNotificationDispatchCallback[Throwable]]()
  private val itsaIDChangedDispatcher = StringProperty("")
  private var updateSharedDataDispatcher:Option[HardwareLoggerSharedData => Unit] = None
  private val tabPreparationEventDispatcher: Dispatcher[TabPreparationEvent => Unit] = Dispatcher()
  private val performBackendValidation: Dispatcher[BackendValidationEvent => Unit] = Dispatcher()
  final case class ListenerResult(name: String, result: ListenerReturnType[Throwable])

  def programModeChanged(mode: ProgramMode)(using ox.Ox): ListenerReturnType[Throwable] =
    ox.either[Throwable, Unit]:
      for callback <- programModeChangedDispatcher.values do
        callback(mode).ok().asInstanceOf[Either[Throwable, Unit]]
      .ok()

      
  def saveDB()(using shardData: Option[HardwareLoggerSharedData] = None)(using ox.Ox): Unit =
    shardData.orElse(NotificationCentre.getFetchedData).foreach(d => saveDBDispatcher.values.foreach(_(d)))
  def duplicateIDWasFound(id: String): Unit = duplicateIDWasFoundDispatcher() = id
 
  
  
  private def createConditionalExecutionFunc[E <: Error](using executionConditionContext: ListenerExecutionConditionContext)(f: () => ListenerReturnType[E]): () => ListenerReturnType[E] =
    () => if executionConditionContext.canExecute then f() else Right(())

  private def createConditionalExecutionFunc[E <: Error, A](using executionConditionContext: ListenerExecutionConditionContext)(f: A => ListenerReturnType[E])(defaultCallback: A => ListenerReturnType[E] = (_: A) => Right(())): A => ListenerReturnType[E] =
    a => if executionConditionContext.canExecute then f(a) else defaultCallback(a)

  private def createConditionalExecutionFunc[E <: Error, A](using executionConditionContext: ListenerExecutionConditionContext)(f: A => ListenerReturnType[E]): A => ListenerReturnType[E] =
    a => if executionConditionContext.canExecute then f(a) else  Right(())





  def programModeChangeNotificationListeners: Dispatcher[ProgramMode => ox.Ox ?=> ListenerReturnType[Throwable]] = programModeChangedDispatcher
  
  def onConnectedSuccessfullyNotificationListeners: Dispatcher[NoArgNotificationDispatchCallback[Throwable]] = onConnectedSuccessfullyDispatcher
  
  def reloadListeners: Dispatcher[ReloadCallback[Throwable]] = reloadDispatcher

  def addProgramModeListener(name: String)(f: ProgramMode => ox.Ox ?=> ListenerReturnType[Throwable])(using executionConditionContext: ListenerExecutionConditionContext): Unit =
    programModeChangedDispatcher += name -> f
  def addDuplicateIDListener(f: String => ListenerReturnType[Throwable])(using executionConditionContext: ListenerExecutionConditionContext): Unit = duplicateIDWasFoundDispatcher.onChange: (op, old, id) =>
    createConditionalExecutionFunc[Throwable, String](f)(id)


  
  def bindToFetchGlobalValidationContext(body: => GlobalValidationContext[? <: HLModel]): Unit =
    fetchGlobalValidationContext = Option(() => body)
    
    
    
  
  def bindToUpdateSharedData(f: HardwareLoggerSharedData => Unit): Unit =
    updateSharedDataDispatcher = Option(f)
    

  def addItsaIDChangedListener(f: String => Unit): Unit =
    itsaIDChangedDispatcher.onChange: (op, old, id) =>
      f(id)

  def addBackendValidationListener(name: String)(f: BackendValidationEvent => Unit)(using executionConditionContext: ListenerExecutionConditionContext)(using GlobalValidationContext[? <: HLModel]): Unit =
    performBackendValidation += name -> f

  
  def bindToFetchData(body: => ListenerReturnType[HardwareGrabberService#Error])(using HardwareGrabberService, HLDatabaseService): Unit =

    if fetchDataListener.isEmpty && body != null then
      fetchDataListener = Option(() => body)

  def bindToGetData(body: => Option[HardwareLoggerSharedData] ): Unit =
    if getFetchedDataListener.isEmpty then
      getFetchedDataListener = Option(() => body)

  
  def getFetchedData: Option[HardwareLoggerSharedData] = getFetchedDataListener.flatMap(_())

  def getGlobalValidationContext: GlobalValidationContext[? <: HLModel] =
    ox.either:
      fetchGlobalValidationContext.map(a => a()).ok()
    .toOption.get
  def sendUpdateSharedDataNotification(sharedData: HardwareLoggerSharedData): Unit = 
    updateSharedDataDispatcher.foreach(_(sharedData))

  def addSaveDBListener[E <: Error](name: String)(f: HardwareLoggerSharedData => ox.Ox ?=> ListenerReturnType[E])(using executionConditionContext: ListenerExecutionConditionContext): Unit =
    saveDBDispatcher += name -> f
  
  def addOnConnectedSuccessfullyListener(name: String)(f: NoArgNotificationDispatchCallback[Throwable])(using executionConditionContext: ListenerExecutionConditionContext): Unit =
    onConnectedSuccessfullyDispatcher += name -> createConditionalExecutionFunc(f)


  def addReloadListener(name: String)(f: ReloadCallback[Throwable])(using executionConditionContext: ListenerExecutionConditionContext): Unit =
    val default = () => (_: ApplicationState) ?=> Right(())
    reloadDispatcher += name -> (
      if executionConditionContext.canExecute then
        f
      else
        default
    )
    
  def reload()(using applicationState: HardwareLoggerSharedData): Unit =
    reloadDispatcher.foreach(_._2())


  def sendBackendValidationEvent(event: BackendValidationEvent)(using ox.Ox)(using context: GlobalValidationContext[HLModel]): Unit =
    performBackendValidation.values.foreach(_(event))

  def sendTabPreparationEvent(event: TabPreparationEvent)(using context: GlobalValidationContext[HLModel]): Unit =
    tabPreparationEventDispatcher.values.foreach(_(event))
  def sendItsaIDChangedNotification(itsaID: String): Unit = itsaIDChangedDispatcher() = itsaID

end NotificationCentre

sealed trait NotificationCentreListener:
  type Error <: Throwable
  
  lazy val identifier: String = NameOf.nameOf[this.type]
end NotificationCentreListener

trait ReloadNotificationListener extends NotificationCentreListener:
  NotificationCentre.addReloadListener(identifier)(reload)
  protected def reload()(using sharedData: HardwareLoggerSharedData): Either[Error, Unit] = Right(())

trait SaveNotificationListener extends NotificationCentreListener:
  NotificationCentre.addSaveDBListener(identifier)(save)
  protected def save(sharedData: HardwareLoggerSharedData)(using ox.Ox): Either[Error, Unit] = Right(())

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

trait DuplicateIDWasFoundNotificationListener extends NotificationCentreListener:
  NotificationCentre.addDuplicateIDListener(duplicateIDWasFound)
  protected def duplicateIDWasFound(itsaID: String): Either[Error, Unit] = Right(())
  
trait ConnectedSuccessfullyNotificationListener extends NotificationCentreListener:
  NotificationCentre.addOnConnectedSuccessfullyListener(identifier)(onConnectedToDBSuccessfully)
  protected def onConnectedToDBSuccessfully(): Either[Error, Unit] = Right(())



trait ItsaIDChangedNotificationListener extends NotificationCentreListener:
  NotificationCentre.addItsaIDChangedListener(onItsaIDChanged)
  protected def onItsaIDChanged(itsaID: String): Unit = ()


trait PerformBackendValidationListener extends NotificationCentreListener:
  self: RequiresGlobalValidationContext =>
  NotificationCentre.addBackendValidationListener(NameOf.nameOf[this.type])(_.registerTabValidation(onBackendValidationFork()))

  protected def onBackendValidationFork(): Unit
end PerformBackendValidationListener



trait RequiresReadingItsaID extends ItsaIDChangedNotificationListener:
  private val currentItsaID = ""
  def itsaID: String
  
  
trait RequiresItsaID extends RequiresReadingItsaID:
  private var currentItsaID: String = ""
  def itsaID: String = currentItsaID
  def itsaID_=(value: String): Unit =
    currentItsaID = value
    NotificationCentre.sendItsaIDChangedNotification(value)

  override final protected def onItsaIDChanged(itsaID: String): Unit =
    currentItsaID = itsaID


trait RequiresGlobalValidationContext:
  given globalValidationContext: GlobalValidationContext[? <: HLModel] = NotificationCentre.getGlobalValidationContext

trait ConnectedSuccessfullyAndReloadListener extends ConnectedSuccessfullyNotificationListener, ReloadNotificationListener


  
  

