package org.itsadigitaltrust.hardwarelogger.viewmodels

import com.somainer.nameof
import org.itsadigitaltrust.common.Operators.{toOption, |>}
import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.common.macros.NameOf
import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.models.HLModel.GeneralInfoModel
import org.itsadigitaltrust.hardwarelogger.services.HardwareGrabberService
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{ConnectedSuccessfullyAndReloadListener, ListenerExecutionConditionContext, NotificationCentre, NotificationName, ProgramModeChangeNotificationListener, SaveNotificationListener}
import org.itsadigitaltrust.hardwarelogger.shared.HardwareLoggerSharedData
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState
import org.scalafx.extras.{BusyWorker, ShowMessage, onFX, showException}
import org.scalafx.extras.BusyWorker.SimpleTask
import ox.either.ok
import ox.{EitherMode, Ox, either}
import scalafx.beans.property.{BooleanProperty, DoubleProperty, ObjectProperty, StringProperty}
import scalafx.scene.Node
import scalafx.Includes.*
import scalafx.scene.Node.sfxNode2jfx
import scalafx.stage.Window
import scalafx.util.Duration

import java.util.concurrent.{CompletableFuture, CompletionException, ExecutionException, Future, TimeUnit}
import scala.collection.mutable
import scala.compiletime.summonFrom
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.boundary.break
import scala.util.{NotGiven, TupledFunction, boundary}


extension [E, T](either: Either[E, T])
  def showIfFailed(): Either[E, T] =
    either.left.map: e =>
      ShowMessage.error("Failed!", "Failed to Run Task", e.toString)
      e


trait BaseViewModel(using HardwareGrabberService) extends HWLLoggable:
  final given hardwareGrabberService: HardwareGrabberService = summon[HardwareGrabberService]
  type Error = HardwareGrabberService#Error
  type VMErrorMode = Either[Error, Unit]


  
end BaseViewModel

trait ViewModel(using applicationState: HardwareLoggerApplicationState) extends BaseViewModel, ConnectedSuccessfullyAndReloadListener, ProgramModeChangeNotificationListener, SaveNotificationListener:
  private given Conversion[Unit, Either[Error, Unit]] = Right.apply

  protected val activeProgramMode: ProgramMode = "Normal"
  
  given executionConditionContext: ListenerExecutionConditionContext = new ListenerExecutionConditionContext:
    override def canExecute: Boolean =
      ProgramMode.mode == activeProgramMode
      

  override lazy val identifier: String = nameof.NameOf.nameOfType[this.type]

    
  protected override def reload()(using HardwareLoggerSharedData): Either[Error, Unit] = ()

  override protected def onConnectedToDBSuccessfully(): Either[Error, Unit] =
    super.onConnectedToDBSuccessfully()
    

  final override protected def programModeChanged(mode: ProgramMode): Either[Error, Unit] =
    onProgramModeChanged(mode)
  protected def onProgramModeChanged(mode: ProgramMode): Either[Error,Unit] =
    ()
  
end ViewModel


trait RootViewModel extends BaseViewModel:

  class RootViewModelBusyWorker(name: String, disabledNodes: Seq[Node]) extends BusyWorker(title = name, disabledNodes = disabledNodes.map(sfxNode2jfx)):
    taskMessageProperty <== progressMessage
    taskProgressProperty <== progressValue


  given taskMessageProperty: StringProperty = StringProperty("")
  given taskProgressProperty: DoubleProperty = DoubleProperty(0)
  given disableNodeWhileTaskRunningProperty: BooleanProperty = BooleanProperty(false)
  var parentWindow: Option[Window] = None
  val disabledNodes: mutable.ListBuffer[Node] = mutable.ListBuffer()
  val viewModels: mutable.ListBuffer[ViewModel] = mutable.ListBuffer()


  extension [E](e: E)
    /** Fail the computation, short-circuiting to the enclosing [[either]] block. */
    transparent inline def failAndShow(): Nothing =
      summonFrom:
        case given boundary.Label[Either[E, Nothing]] =>
          ShowMessage.error("Failed!", "Failed to Run Task", e.toString)
          break(Left(e))
        case given boundary.Label[Either[Nothing, Nothing]] =>
          scala.compiletime.error("The enclosing `either` call uses a different error type.\nIf it's explicitly typed, is the error type correct?")
  end extension
end RootViewModel





    



  
  
