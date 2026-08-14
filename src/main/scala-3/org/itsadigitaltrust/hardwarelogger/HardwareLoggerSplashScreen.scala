package org.itsadigitaltrust.hardwarelogger

import javafx.beans.value.ObservableValue
import org.itsadigitaltrust.hardwarelogger.backend.HLDBErrorCode.ConnectionError
import org.itsadigitaltrust.hardwarelogger.core.HWLoggerShowMessage
import org.itsadigitaltrust.hardwarelogger.core.ui.*
import org.itsadigitaltrust.hardwarelogger.services.{HLDatabaseService, HLDatabaseServiceException, HardwareGrabberService, HardwareIDValidationService}
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState
import org.itsadigitaltrust.hardwarelogger.tasks.DelegatedSimpleTask
import org.scalafx.extras
import org.scalafx.extras.BusyWorker.SimpleTask
import org.scalafx.extras.{BusyWorker, ShowMessage}
import ox.{Ox, OxError}
import ox.either.{fail, ok}
import scalafx.application.Platform
import scalafx.concurrent.WorkerStateEvent
import scalafx.scene.control.ProgressBar
import scalafx.scene.image.ImageView

import java.util.concurrent.Future
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.boundary

trait SplashScreenViewModel[E <: Throwable, R] extends HWLoggerShowMessage, ox.OxErrorModeSimpleTask[E, R]:

  override val exitIfFailed: Boolean = true

  given progressBarValue: DoubleProperty = DoubleProperty(0.0)

  given statusLabelText: StringProperty = StringProperty("")

  protected given totalNumberOfItems: Int = scala.compiletime.deferred
  protected val loadTimeout: FiniteDuration = 5000.millisecond





  final def load(): Unit =
    val worker = new BusyWorker("Initialise Application", None):
      progressBarValue <== progressValue
      statusLabelText <== progressMessage

    worker.doTask(this)

  end load



  protected def onLoad(): Unit


  override def onFinished(result: Either[E, R]): Unit =
    onLoad()
  override def onFailed(e: WorkerStateEvent): Unit =
    super.onFailed(e)

end SplashScreenViewModel


final class HardwareLoggerSplashScreenViewModel(parent: Option[Window] = None)(using val databaseService: HLDatabaseService, hardwareGrabberService: HardwareGrabberService, hardwareLoggerIDValidationService: HardwareIDValidationService, showMessage: HWLoggerShowMessage, applicationState: HardwareLoggerApplicationState)(@FunctionalInterface onDataLoaded: HardwareLoggerApplicationState => Unit) extends SplashScreenViewModel[Throwable, Unit]:
  override protected given totalNumberOfItems: Int = 8


  override def onCall()(using context: OxError[Throwable, [X] =>> Either[Throwable, X]]): Either[Throwable, Unit] =
    var index = 0D
    val task = this
    val result = ox.supervisedError(ox.EitherMode[Throwable]):
      ox.forkUserError:
        ox.either:
          extras.onFX:
            task.message() = "Connecting to database..."
          databaseService.connect()
      .join()
      logger.info("Connected to database!")
      index += 1

      extras.onFX:
        task.progress() = index /  totalNumberOfItems

        task.message() = "Loading data..."

      ox.forkUserError:
        ox.either:
          index = applicationState.reload(task, index).ok()
          ()
      .join()

      Right(())
    end result
    Platform.runLater:
      task.message() = "Finished"
      task.progress() = 1.0D
    result
  end onCall

  override def onLoad(): Unit =
    onDataLoaded(applicationState)


  override protected def parentWindow: Option[Window] = None

end HardwareLoggerSplashScreenViewModel


class HardwareLoggerSplashScreen[E <: Throwable, R, VM <: SplashScreenViewModel[E, R]](splashWidth: Double, splashHeight: Double, viewModel: VM)(onDataLoaded: () => Unit)(using applicationState: HardwareLoggerApplicationState) extends VBox:
  maxWidth() = splashWidth
  maxHeight() = splashHeight
  private val splashImage = new ImageView(getClass.getResource("assets/images/ITSA_Logo.png").toString):
    fitWidth = splashWidth
    fitHeight = splashHeight * 0.70

    preserveRatio = true
    smooth = true

  private val progressBar = new ProgressBar():
    progress <== viewModel.progressBarValue
    minWidth <== width
    prefWidth = splashWidth

  private val statusLabel = new Label("Loading..."):
    text <== viewModel.statusLabelText
    styleClass += "status-label"
    font = Font.font(13D)
    
      
  private val progressContainer = new VBox(10, progressBar, statusLabel):
    padding = Insets(10, 5D, 5D, 5D)
    
  private val rootLayout = new VBox:
    styleClass += "root-layout"
    children = List(splashImage, progressContainer)

  styleClass += "splash-screen"
  children += rootLayout
  stylesheets += "org/itsadigitaltrust/hardwarelogger/views/stylesheets/splashscreen.css"


  final def load(): Unit =
    viewModel.load()

end HardwareLoggerSplashScreen



