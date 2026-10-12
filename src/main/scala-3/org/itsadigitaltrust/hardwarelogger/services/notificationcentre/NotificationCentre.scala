package org.itsadigitaltrust.hardwarelogger.services.notificationcentre

import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.models.HLModel
import org.itsadigitaltrust.hardwarelogger.services.validation.GlobalValidationContext
import org.itsadigitaltrust.hardwarelogger.services.HardwareGrabberService
import org.itsadigitaltrust.hardwarelogger.services.database.HLDatabaseService
import org.itsadigitaltrust.hardwarelogger.shared.HardwareLoggerSharedData
import org.scalafx.extras.BusyWorker.SimpleTask
import ox.{Ox, Fork}
import ox.either.ok
import scalafx.beans.property.StringProperty

import scala.collection.mutable
import scala.reflect.ClassTag
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import scala.jdk.CollectionConverters.*

// ==========================================
// Strongly-Typed System Event Definitions
// ==========================================
sealed trait SystemEvent

final case class StartReloadEvent(task: SimpleTask[?]) extends SystemEvent
case object ReloadViewModelsEvent extends SystemEvent
final case class ProgramModeChangedEvent(mode: ProgramMode) extends SystemEvent
final case class SaveToDBEvent()(using val context: GlobalValidationContext[HLModel])(using val oxCtx: Ox) extends SystemEvent with ForkBasedEvent[Unit]
final case class BackendValidationEvent()(using val context: GlobalValidationContext[HLModel])(using val oxCtx: Ox) extends SystemEvent with ForkBasedEvent[Unit]
final case class ItsaIDChangedEvent(itsaID: String) extends SystemEvent
final case class DuplicateIDFoundEvent(itsaID: String) extends SystemEvent
case object ConnectedSuccessfullyEvent extends SystemEvent
case object HardDriveIDsChangedEvent extends SystemEvent

// Helper trait to allow concurrent tasks to load asynchronously via Ox forks
trait ForkBasedEvent[R]:
  protected val runningForks = new CopyOnWriteArrayList[Fork[R]]()
  def registerCallback(block: => R)(using Ox): Unit = runningForks.add(ox.fork(block))
  def +=(block: => R)(using Ox): Unit = registerCallback(block)
  def apply()(using Ox): List[R] = runningForks.asScala.toList.map(_.join())

// Execution gating context structure
trait ListenerExecutionConditionContext:
  def canExecute: Boolean
object ListenerExecutionConditionContext:
  given defaultContext: ListenerExecutionConditionContext = new ListenerExecutionConditionContext:
    override def canExecute: Boolean = true

// ==========================================
// Central Event Bus Framework (NotificationCentre)
// ==========================================
object NotificationCentre extends HWLLoggable:

  // Thread-safe map for references
  private val stateRef = new ConcurrentHashMap[Class[?], () => Any]()

  // Centralized Event Bus Registry using a ConcurrentHashMap of CopyOnWriteArrayList elements.
  // This guarantees modifications (.subscribe) do not crash ongoing loops (.publish).
  private val listenersRegistry = new ConcurrentHashMap[Class[?], CopyOnWriteArrayList[Any => Any]]()

  // JavaFX string bindings explicitly exposed for boundary UI listeners
  private val duplicateIDProperty = StringProperty("")
  private val itsaIDChangedProperty = StringProperty("")

  // Subscribe a component listener to a specific Event variant channel
  def subscribe[E <: SystemEvent : ClassTag](listener: E => Any): Unit =
    val clazz = implicitly[ClassTag[E]].runtimeClass
    val list = listenersRegistry.computeIfAbsent(clazz, _ => new CopyOnWriteArrayList[Any => Any]())
    list.add(listener.asInstanceOf[Any => Any])

  // Broadcast an instantiated event package directly down to all active channels
  def publish[E <: SystemEvent](event: E): Unit =
    val clazz = event.getClass
    val targetList = listenersRegistry.get(clazz)

    if targetList != null then
      // Taking a snapshot of the list buffer. Any background thread calling subscribe
      // will modify a new list array copy under the hood, making this snapshot safe to loop over.
      val listenersSnapshot = targetList.asScala.toSeq
      listenersSnapshot.foreach(_.apply(event))

  // ==========================================
  // Functional State Mappings Providers
  // ==========================================

  def registerDataProvider(provider: => HardwareLoggerSharedData): Unit =
    stateRef.put(classOf[HardwareLoggerSharedData], () => provider)

  def registerValidationContextProvider(provider: => GlobalValidationContext[? <: HLModel]): Unit =
    stateRef.put(classOf[GlobalValidationContext[?]], () => provider)

  def getFetchedData: Option[HardwareLoggerSharedData] =
    Option(stateRef.get(classOf[HardwareLoggerSharedData])).map(_.apply().asInstanceOf[HardwareLoggerSharedData])

  def getGlobalValidationContext: GlobalValidationContext[? <: HLModel] =
    Option(stateRef.get(classOf[GlobalValidationContext[?]]))
      .map(_.apply().asInstanceOf[GlobalValidationContext[? <: HLModel]])
      .getOrElse(GlobalValidationContext.globalValidationContext)

  // ==========================================
  // Custom API Legacy Bridges / FX Properties
  // ==========================================

  def sendItsaIDChangedNotification(itsaID: String): Unit =
    itsaIDChangedProperty() = itsaID
    publish(ItsaIDChangedEvent(itsaID))

  def duplicateIDWasFound(id: String): Unit =
    duplicateIDProperty() = id
    publish(DuplicateIDFoundEvent(id))

  // Entry point called by external controllers / ViewModels to launch reloading cycles
  def reload(potentialTask: Option[SimpleTask[?]] = None): Option[HardwareLoggerSharedData] =
    potentialTask.map: task =>
      publish(StartReloadEvent(task))
    .getOrElse:
      logger.warn("Potential loading UI task context missing.")
    getFetchedData

  def executeBackendValidation()(using Ox)(using GlobalValidationContext[HLModel]): Unit =
    val event = BackendValidationEvent()
    publish(event)
    event()

  def executeSaveToDBEvent()(using Ox)(using GlobalValidationContext[HLModel]): Unit =
    val event = SaveToDBEvent()
    publish(event)
    event()
    
  def startReload(task: SimpleTask[?]): Unit =
    publish(StartReloadEvent(task))

end NotificationCentre

// ==========================================
// Trait-Driven Event Listener Hooks Mixins
// ==========================================

sealed trait NotificationCentreListener:
  type Error <: Throwable

trait ReloadNotificationListener extends NotificationCentreListener:
  NotificationCentre.subscribe[ReloadViewModelsEvent.type](_ => reload())
  protected def reload(): Either[Error, Unit] = Right(())

trait SaveNotificationListener extends NotificationCentreListener:
  import ox.either.orThrow
  NotificationCentre.subscribe[SaveToDBEvent]: event =>
    given Ox = event.oxCtx
    event `+=`:
      try save().orThrow catch case ex: Throwable => throw ex

  protected def save()(using Ox): Either[Error, Unit] = Right(())

trait ProgramModeChangeNotificationListener extends NotificationCentreListener:
  NotificationCentre.subscribe[ProgramModeChangedEvent](ev => programModeChanged(ev.mode))
  protected def programModeChanged(mode: ProgramMode): Either[Error, Unit] = Right(())

trait DuplicateIDWasFoundNotificationListener extends NotificationCentreListener:
  NotificationCentre.subscribe[DuplicateIDFoundEvent](ev => duplicateIDWasFound(ev.itsaID))
  protected def duplicateIDWasFound(itsaID: String): Either[Error, Unit] = Right(())

trait ConnectedSuccessfullyNotificationListener extends NotificationCentreListener:
  NotificationCentre.subscribe[ConnectedSuccessfullyEvent.type](_ => onConnectedToDBSuccessfully())
  protected def onConnectedToDBSuccessfully(): Either[Error, Unit] = Right(())

trait ItsaIDChangedNotificationListener extends NotificationCentreListener:
  NotificationCentre.subscribe[ItsaIDChangedEvent](ev => onItsaIDChanged(ev.itsaID))
  protected def onItsaIDChanged(itsaID: String): Unit = ()

trait PerformBackendValidationListener extends NotificationCentreListener, RequiresGlobalValidationContext:
  NotificationCentre.subscribe[BackendValidationEvent]: event =>
    given Ox = event.oxCtx
    event.registerCallback(onBackendValidationFork())

  protected def onBackendValidationFork()(using Ox): Unit

trait RequiresReadingItsaID extends ItsaIDChangedNotificationListener:
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
