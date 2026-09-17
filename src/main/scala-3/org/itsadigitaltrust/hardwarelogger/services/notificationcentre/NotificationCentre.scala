package org.itsadigitaltrust.hardwarelogger.services.notificationcentre

import org.itsadigitaltrust.common.Operators.??
import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.common.macros.NameOf
import org.itsadigitaltrust.common.orThrowOption
import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.models.HLModel
import org.itsadigitaltrust.hardwarelogger.services.validation.GlobalValidationContext
import org.itsadigitaltrust.hardwarelogger.services.HardwareGrabberService
import org.itsadigitaltrust.hardwarelogger.services.database.HLDatabaseService
import org.itsadigitaltrust.hardwarelogger.shared.HardwareLoggerSharedData
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState
import org.scalafx.extras.BusyWorker.SimpleTask
import ox.{Ox, OxError, either}
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
  case StartReload extends NotificationName
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

trait ForkBasedEvent[R](using context: GlobalValidationContext[HLModel]):
  protected val runningForks = mutable.ListBuffer[ox.Fork[R]]()

  /**
   * Tabs spawn a virtual thread fork and register it directly into the event buffer.
   */
  def registerCallback(block: => R)(using ox.Ox): Unit =
    val tabFork = ox.fork(block)
    runningForks += tabFork


  def += (block: => R)(using ox.Ox): Unit =
    registerCallback(block)

  def getForks(using ox.Ox): List[ox.Fork[R]] =
    runningForks.toList

  def apply()(using ox.Ox): List[R] =
    getForks.map(_.join())

end ForkBasedEvent



final case class BackendValidationEvent()(using context: GlobalValidationContext[HLModel])(using ox.Ox) extends ForkBasedEvent[Unit]
final case class SaveToDBEvent()(using context: GlobalValidationContext[HLModel])(using ox.Ox) extends ForkBasedEvent[Unit]

object NotificationCentre extends HWLLoggable:
  type Error = Throwable //HardwareGrabberService#Error
  private type ListenerReturnType[E <: Error] = Either[E, Unit]
  private type NoArgNotificationDispatchCallback[E <: Error] = () => ListenerReturnType[E]
  private type ApplicationState = HardwareLoggerSharedData
  private type ReloadCallback[E <: Error] = () => ListenerReturnType[E]
  private type StartReloadCallback[T <: SimpleTask[R], R,  E <: Error]

  type ReturnValue[C] = C match
    case Function0[r] => r
    case Function1[_, r] => r
    case Function1[_, ContextFunction1[_, r]] => r

  class GenericDispatcher[C, R]:
    type Callback = C
    type Return = R

    val listeners = mutable.ListBuffer[Callback]()

    def foreach(f: Callback => Unit): Unit = listeners.foreach(f)

    def add(f: Callback): Unit = listeners += f

    def remove(f: Callback): Unit = listeners -= f

    def +=(f: Callback): this.type =
      add(f)
      this

    def ++=(fs: Seq[Callback]): this.type =
      listeners ++= fs
      this

    def map[B](f: Callback => B): Seq[B] =
      listeners.map(f).toSeq

    def size: Int = listeners.size

  end GenericDispatcher

  final class Dispatcher[C] extends GenericDispatcher[C, ReturnValue[C]]



  private var fetchDataListener: Option[NoArgNotificationDispatchCallback[HardwareGrabberService#Error]] = None
  private var fetchGlobalValidationContext: Option[() => GlobalValidationContext[? <: HLModel]] = None

  private var getFetchedDataListener: Option[() => Option[HardwareLoggerSharedData]] = None

  private val startReloadDispatcher: Dispatcher[SimpleTask[?] => OxError[Throwable, [X] =>> Either[Throwable, X]] ?=> Unit] = Dispatcher()
  private val reloadDispatcher = Dispatcher[ReloadCallback[Throwable]]()
  private val programModeChangedDispatcher = Dispatcher[ProgramMode => ox.Ox ?=> ListenerReturnType[Throwable]]()
  private val saveDBDispatcher = Dispatcher[SaveToDBEvent => ox.Ox ?=> Unit]()
  private val duplicateIDWasFoundDispatcher= StringProperty("")
  private val onConnectedSuccessfullyDispatcher = Dispatcher[NoArgNotificationDispatchCallback[Throwable]]()
  private val itsaIDChangedDispatcher = StringProperty("")
  private var updateSharedDataDispatcher:Option[HardwareLoggerSharedData => Unit] = None
  private val performBackendValidation: Dispatcher[BackendValidationEvent => Ox ?=> Unit] = Dispatcher()
  final case class ListenerResult(name: String, result: ListenerReturnType[Throwable])

  def programModeChanged(mode: ProgramMode)(using ox.Ox): ListenerReturnType[Throwable] =
    ox.either[Throwable, Unit]:
      for callback <- programModeChangedDispatcher do
        callback(mode).ok()



  def saveDB(event: SaveToDBEvent)(using shardData: Option[HardwareLoggerSharedData] = None)(using ox.Ox): Unit =
    saveDBDispatcher.foreach(_.apply(event))
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

  def addProgramModeListener()(f: ProgramMode => ox.Ox ?=> ListenerReturnType[Throwable])(using executionConditionContext: ListenerExecutionConditionContext): Unit =
    programModeChangedDispatcher +=  f
  def addDuplicateIDListener(f: String => ListenerReturnType[Throwable])(using executionConditionContext: ListenerExecutionConditionContext): Unit = duplicateIDWasFoundDispatcher.onChange: (op, old, id) =>
    createConditionalExecutionFunc[Throwable, String](f)(id)



  def bindToFetchGlobalValidationContext(body: => GlobalValidationContext[? <: HLModel]): Unit =
    fetchGlobalValidationContext = Option(() => body)




  def bindToUpdateSharedData(f: HardwareLoggerSharedData => Unit): Unit =
    updateSharedDataDispatcher = Option(f)


  def addItsaIDChangedListener(f: String => Unit): Unit =
    itsaIDChangedDispatcher.onChange: (op, old, id) =>
      f(id)

  def addBackendValidationListener()(f: BackendValidationEvent => Ox ?=> Unit)(using executionConditionContext: ListenerExecutionConditionContext)(using GlobalValidationContext[? <: HLModel]): Unit =
    performBackendValidation += f


  def bindToFetchData(body: => ListenerReturnType[HardwareGrabberService#Error])(using HardwareGrabberService, HLDatabaseService): Unit =

    if fetchDataListener.isEmpty && body != null then
      fetchDataListener = Option(() => body)

  def bindToGetData(body: => Option[HardwareLoggerSharedData] ): Unit =
    if getFetchedDataListener.isEmpty then
      getFetchedDataListener = Option(() => body)


  def getFetchedData: Option[HardwareLoggerSharedData] = getFetchedDataListener.flatMap(_())

  def getGlobalValidationContext: GlobalValidationContext[? <: HLModel] =
    GlobalValidationContext.globalValidationContext
  def sendUpdateSharedDataNotification(sharedData: HardwareLoggerSharedData): Unit =
    updateSharedDataDispatcher.foreach(_(sharedData))

  def addSaveDBListener[E <: Error]()(f: SaveToDBEvent => ox.Ox ?=> Unit)(using executionConditionContext: ListenerExecutionConditionContext): Unit =
    saveDBDispatcher += f

  def addOnConnectedSuccessfullyListener()(f: NoArgNotificationDispatchCallback[Throwable])(using executionConditionContext: ListenerExecutionConditionContext): Unit =
    onConnectedSuccessfullyDispatcher += createConditionalExecutionFunc(f)


  def addReloadListener()(f: ReloadCallback[Throwable])(using executionConditionContext: ListenerExecutionConditionContext): Unit =
    val default = () => Right(())
    reloadDispatcher +=  (
      if executionConditionContext.canExecute then
        f
      else
        default
    )


  def addStartReloadListener(callback: startReloadDispatcher.type#Callback): Unit =
    startReloadDispatcher += callback

  def reload(potentialTask: Option[SimpleTask[?]] = None)(using context: OxError[Throwable, [X] =>> Either[Throwable, X]]): Option[ApplicationState] =
    potentialTask.map: task => 
      startReloadDispatcher.foreach(_.apply(task))
      reloadDispatcher.foreach(_.apply)
    .getOrElse:
      logger.warn("potentialTask is None")
      
    getFetchedData
      
      

  def executeBackendValidation()(using ox.Ox)(using context: GlobalValidationContext[HLModel]): Unit =
    val event = BackendValidationEvent()
    sendBackendValidationEvent(event)
    event()
    
  def executeSaveToDBEvent()(using ox.Ox)(using context: GlobalValidationContext[HLModel], sharedData: Option[HardwareLoggerSharedData] = None): Unit =
    val event = SaveToDBEvent()
    saveDB(event)
    event()



  def sendBackendValidationEvent(event: BackendValidationEvent)(using ox.Ox)(using context: GlobalValidationContext[HLModel]): Unit =
    performBackendValidation.foreach(_(event))

  def sendItsaIDChangedNotification(itsaID: String): Unit = itsaIDChangedDispatcher() = itsaID

end NotificationCentre

sealed trait NotificationCentreListener:
  type Error <: Throwable

end NotificationCentreListener

trait ReloadNotificationListener extends NotificationCentreListener:
  NotificationCentre.addReloadListener()(reload)
  protected def reload(): Either[Error, Unit] = Right(())

trait SaveNotificationListener extends NotificationCentreListener:
  import ox.either.orThrow
  NotificationCentre.addSaveDBListener(): e =>

    e `+=`:
      try
        save().orThrow
      catch
        case e: Throwable => throw e

  protected def save()(using ox.Ox): Either[Error, Unit] = Right(())

trait ProgramModeChangeNotificationListener extends NotificationCentreListener:
  NotificationCentre.addProgramModeListener()(programModeChanged)

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
  NotificationCentre.addOnConnectedSuccessfullyListener()(onConnectedToDBSuccessfully)
  protected def onConnectedToDBSuccessfully(): Either[Error, Unit] = Right(())



trait ItsaIDChangedNotificationListener extends NotificationCentreListener:
  NotificationCentre.addItsaIDChangedListener(onItsaIDChanged)
  protected def onItsaIDChanged(itsaID: String): Unit = ()


trait PerformBackendValidationListener extends NotificationCentreListener, RequiresGlobalValidationContext:
  NotificationCentre.addBackendValidationListener(): event =>
     event.registerCallback(onBackendValidationFork())

  protected def onBackendValidationFork()(using ox.Ox): Unit
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


  
  

