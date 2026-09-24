package org.itsadigitaltrust.hardwarelogger.viewmodels

import com.somainer.nameof
import org.itsadigitaltrust.common.Operators.{toOption, |>}
import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.common.macros.NameOf
import org.itsadigitaltrust.hardwarelogger.core.{HWLoggerShowMessage, ShowMessageHWLogger}
import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.models.HLModel
import org.itsadigitaltrust.hardwarelogger.models.HLModel.GeneralInfoModel
import org.itsadigitaltrust.hardwarelogger.mvvm.ModelWrapper
import org.itsadigitaltrust.hardwarelogger.services.HardwareGrabberService
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{ConnectedSuccessfullyAndReloadListener, ListenerExecutionConditionContext, NotificationCentre, NotificationName, ProgramModeChangeNotificationListener, SaveNotificationListener}
import org.itsadigitaltrust.hardwarelogger.shared.HardwareLoggerSharedData
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState
import org.itsadigitaltrust.hardwarelogger.tasks.{HardwareLoggerBusyWorker, HardwareLoggerSimpleTask}
import org.scalafx.extras.{BusyWorker, ShowMessage, onFX, showException}
import org.scalafx.extras.BusyWorker.SimpleTask
import ox.either.ok
import ox.{EitherMode, Ox, OxError, either}
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


trait BaseViewModel extends HWLLoggable:
  type Error = HardwareGrabberService#Error
  type VMErrorMode = Either[Error, Unit]


  
end BaseViewModel

trait RootViewModel extends BaseViewModel:
  
  given taskMessageProperty: StringProperty = StringProperty("")
  given taskProgressProperty: DoubleProperty = DoubleProperty(0)
  given disableNodeWhileTaskRunningProperty: BooleanProperty = BooleanProperty(false)

  def doWork[E <: Throwable, R](numberOfOperations: Int = 1, timeout: FiniteDuration = 3.seconds, name: String = "")(using showMessage:HWLoggerShowMessage)(body: HardwareLoggerSimpleTask[E, R] ?=> OxError[E, [X] =>> Either[E, X]] ?=> Either[E, R]) : Either[E, R] =
    new HardwareLoggerBusyWorker(name, window = None)(using statusMessageProp = Option(taskMessageProperty), progressProp = Option(taskProgressProperty), busyProp = Option(disableNodeWhileTaskRunningProperty))
    .doTask(name):
      new HardwareLoggerSimpleTask[E, R](numberOfOperations, timeout):
        override def onCall()(using context: OxError[E, [X] =>> Either[E, X]]): Either[E, R] =
          given task: HardwareLoggerSimpleTask[E, R] = this
          body
    .get



  end doWork



end RootViewModel


trait ViewModel extends BaseViewModel, ConnectedSuccessfullyAndReloadListener, ProgramModeChangeNotificationListener, SaveNotificationListener:

  protected val activeProgramMode: ProgramMode = "Normal"


  protected override def reload(): Either[Error, Unit] = Right(())

  override protected def onConnectedToDBSuccessfully(): Either[Error, Unit] =
    super.onConnectedToDBSuccessfully()


  final override protected def programModeChanged(mode: ProgramMode): Either[Error, Unit] =
    onProgramModeChanged(mode)
  protected def onProgramModeChanged(mode: ProgramMode): Either[Error,Unit] =
    Right(())

end ViewModel


trait ViewModelWithWrapper[M <: HLModel](final val model: M) extends ViewModel:
  protected val wrapper: ModelWrapper[M] = ModelWrapper(model)
  
  


    



  
  
