package org.itsadigitaltrust.hardwarelogger.state

import org.itsadigitaltrust.common.Operators.{??, ???, toOption, |>}
import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.common.{forkJoin, forkUserJoin, orThrowOption}
import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.models.HLModel
import org.itsadigitaltrust.hardwarelogger.models.HLModel.*
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.NotificationCentre
import org.itsadigitaltrust.hardwarelogger.services.validation.GlobalValidationContext
import org.itsadigitaltrust.hardwarelogger.services.{HLDatabaseService, HLDatabaseServiceException, HardwareGrabberService}
import org.itsadigitaltrust.hardwarelogger.shared.HardwareLoggerSharedData
import org.itsadigitaltrust.hardwarelogger.tasks.HardwareLoggerSimpleTask
import org.itsadigitaltrust.hardwarelogger.views.Reloadable
import org.scalafx.extras
import org.scalafx.extras.BusyWorker.SimpleTask
import ox.Ox
import ox.either.{catching, fail, ok, orThrow}
import scalafx.application.Platform

import scala.::
import scala.languageFeature.implicitConversions
import scala.Conversion.into
import scala.concurrent.duration.DurationInt
import scala.math.Fractional.Implicits.infixFractionalOps
import scala.math.Integral.Implicits.infixIntegralOps

private[state] type Data = HardwareLoggerSharedData
case class HardwareLoggerApplicationState()( using HardwareGrabberService, HLDatabaseService) extends HWLLoggable:
  private var data: Data = HardwareLoggerSharedData()
  private val globalValidationContext: GlobalValidationContext[? <: HLModel] = GlobalValidationContext()
  
  def getData: Data = data
  def generalInfo: Option[Seq[GeneralInfoModel]] = data.info

  def memory: Option[Seq[MemoryModel]] =
    data.memory

  def hardDrives: Option[Seq[HardDriveModel]] = data.drives

  def media: Option[Seq[MediaModel]] =
    data.media

  def processors: Option[Seq[ProcessorModel]] = data.processors

  def itsaID: Option[String] = data.itsaID

  def itsaID_=(id: into[Option[String]]): Unit =
    data = data.copy(itsaID = id)
    itsaID.foreach(NotificationCentre.sendItsaIDChangedNotification)

  def reload()(using hardwareGrabberService: HardwareGrabberService, databaseService: HLDatabaseService): Either[HardwareGrabberService#Error, Unit] =
    given [A]: Conversion[A, Option[A]] = Option.apply
      ox.supervisedError(ox.EitherMode[HardwareGrabberService#Error]):
        ox.either[HardwareGrabberService#Error, HardwareLoggerSharedData]:
          val infoFork = ox.fork(hardwareGrabberService.generalInfo)
          val drivesFork = ox.fork(hardwareGrabberService.hardDrives)
          val processorsFork = ox.fork(hardwareGrabberService.processors)
          val memoryFork = ox.fork(hardwareGrabberService.memory)
          val mediaFork = ox.fork(hardwareGrabberService.media)

          val info = infoFork.join().ok()
          val drives = drivesFork.join().ok()
          val processors = processorsFork.join().ok()
          val memory = memoryFork.join().ok()
          val media = mediaFork.join().toOption ??? Seq()
          data = data.copy(
            info = info,
            drives = drives,
            processors = processors,
            memory = memory,
            media = media,
            itsaID = data.itsaID
          )
          data
        Right(())




    //    val itsaID = generalInfo.flatMap(_.headOption.flatMap(_.serial |> databaseService.findItsaIdBySerialNumber)).flatMap(_.orThrowOption)


  end reload

  /**
   *
   * @param task
   * @param hardwareGrabberService
   * @param databaseService
   * @param totalNumberOfItems
   * @param currentIndex
   * @param x$6
   * @tparam T
   * @tparam R
   * @return The new index for the current part of the task it is at.
   */
  def reload[T <: SimpleTask[R], R](task: T, currentIndex: Double = 0D)(using hardwareGrabberService: HardwareGrabberService, databaseService: HLDatabaseService, totalNumberOfItems: Int = 0)(using Ox): Either[Throwable, Double] =
    val mode = ox.EitherMode[Throwable]()
    ox.supervisedError(mode):
      var index = currentIndex
      def performAsTask(msg: String)(using allowedMode: ProgramMode = "Normal")(body: scala.util.boundary.Label[Either[Throwable, Unit]] ?=> Either[Throwable, Any]) =
        ox.forkUserError:
          ox.either:
            if ProgramMode.mode != allowedMode && allowedMode != "Both" then
              ()
            else
              Platform.runLater:
                task.message() = msg

              body.map: res =>
                index += 1

                Platform.runLater(task.progress() = index / totalNumberOfItems)
                res
        .join()
      end performAsTask


      performAsTask("Loading info...")(using allowedMode = "Both"):
        val option = hardwareGrabberService.loadGeneralInfo()
        data = data.copy(info = option.ok().toOption)
        option

      performAsTask("Checking for ITSA ID..."):
        // No fork here: `.ok()` breaks to the `ox.either` label created in `performAsTask`, which
        // lives on this thread. Breaking from inside a fork lets the `boundary.Break` escape on the
        // fork's thread and surface as `ExecutionException: scala.util.boundary$Break`.
        // The explicit `[GeneralInfoModel]` matters too: without it `M` is minimised to `Nothing`,
        // no mapper can be summoned and the lookup always fails with "no entry found".
        // A PC that is not in the database yet is normal, so a failed lookup means "no ID", not an error.
        val id = data.info
          .flatMap(_.headOption)
          .flatMap: info =>
            databaseService.findBySerialNumber[GeneralInfoModel](info.serial).toOption.flatMap(_.itsaID)

        data = data.copy(itsaID = id)
        if ProgramMode.mode == "Normal" then
          data = data.copy(info = data.info.map(_.map(_.copy(itsaID = id))))
        Right(id)

      performAsTask("Loading memory..."):
        val value = hardwareGrabberService.loadMemory()
        data = data.copy(memory = value.ok().toOption)
        value

      performAsTask("Loading drives...")(using allowedMode = "Both"):
        val value = hardwareGrabberService.loadHardDrives()
        data = data.copy(drives = value.ok().toOption)
        value

      performAsTask("Loading processors..."):
        val processors = hardwareGrabberService.loadProcessors()
        logger.info(s"Loaded processors $processors")
        data = data.copy(processors = processors.ok().toOption)
        processors

      performAsTask("Loading media..."):
        val media = hardwareGrabberService.loadMedia()
        data = data.copy(media = media.ok().toOption)
        media


      Right(index)
  end reload

  NotificationCentre.bindToFetchData:
    ox.supervisedError(ox.EitherMode[HardwareGrabberService#Error]):
      ox.forkUserError:
        ox.either(reload())
      .join()
//      Right(NotificationCentre.reload()(using data))

  NotificationCentre.bindToGetData:
    Option(data)

  NotificationCentre.bindToFetchGlobalValidationContext(globalValidationContext)
  
  NotificationCentre.bindToUpdateSharedData: newData =>
    val currentData = this.data
    if newData.info.isDefined then
      data = data.copy(info = newData.info)
    if newData.drives.isDefined then
      data = data.copy(drives = newData.drives)
    if newData.processors.isDefined then
      data = data.copy(processors = newData.processors)
    if newData.memory.isDefined then
      data = data.copy(memory = newData.memory)
    if newData.drives.isDefined then
      data = data.copy(drives = newData.drives)
    if newData.media.isDefined then
      data = data.copy(media = newData.media)
    if newData.itsaID.isDefined then
      data = data.copy(itsaID = newData.itsaID)
    
        
    





end HardwareLoggerApplicationState
