package org.itsadigitaltrust.hardwarelogger.state

import org.itsadigitaltrust.common.Operators.{?, ??, toOption, |>}
import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.common.processes.tlp.TlpStat
import org.itsadigitaltrust.common.{forkJoin, forkUserJoin, orThrowOption}
import org.itsadigitaltrust.hardwarelogger.backend.database.HLDatabase
import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.models.*
import org.itsadigitaltrust.hardwarelogger.services.OshiHardwareGrabberApplicationService.HardwareGrabberException
import org.itsadigitaltrust.hardwarelogger.services.database.mappers.HLModelToBackendMappers
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{NotificationCentre, PerformBackendValidationListener, RequiresGlobalValidationContext}
import org.itsadigitaltrust.hardwarelogger.services.validation.GlobalValidationContext
import org.itsadigitaltrust.hardwarelogger.services.{HLDatabaseService, HLDatabaseServiceException, HardwareGrabberService}
import org.itsadigitaltrust.hardwarelogger.shared.HardwareLoggerSharedData
import org.itsadigitaltrust.hardwarelogger.tasks.HardwareLoggerSimpleTask
import org.itsadigitaltrust.hardwarelogger.core.DataTypes.*
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
import scala.math.Numeric.Implicits.infixNumericOps
import org.itsadigitaltrust.common.types.*
import org.itsadigitaltrust.hardwarelogger.core.BuildInfo

private[state] type Data = HardwareLoggerSharedData

case class HardwareLoggerApplicationState()(using HardwareGrabberService, HLDatabaseService) extends HWLLoggable, RequiresGlobalValidationContext:
  private var data: Data = HardwareLoggerSharedData()
  private val globalValidationContext: GlobalValidationContext[? <: HLModel] = GlobalValidationContext.globalValidationContext

  def getData: Data = data
  def generalInfo: Option[Seq[GeneralInfoModel]] = data.info
  def memory: Option[Seq[MemoryModel]] = data.memory
  def hardDrives: Option[Seq[HardDriveModel]] = data.drives
  def media: Option[Seq[MediaModel]] = data.media
  def processors: Option[Seq[ProcessorModel]] = data.processors
  def itsaID: Option[String] = data.itsaID
  def oldItsaID: Option[String] = data.oldItsaID
  def battery: Option[Seq[BatteryStatusModel]] = data.battery

  // ==========================================
  // Extracted Component Reload Options (Thread-Safe)
  // ==========================================

  // This is executed last (as it depends on everything else to be loaded)
  private def reloadGeneralInfo()(using hardwareGrabberService: HardwareGrabberService, databaseService: HLDatabaseService)(using Ox): Either[Throwable, Seq[GeneralInfoModel]] =
    ox.either:
      val option: Seq[GeneralInfoModel] = hardwareGrabberService.loadGeneralInfo().ok()
        val updatedOption =
          option.headOption.map: info =>
            val issues: Seq[GeneralDriveStatus] =
              val drives = hardDrives ?? Seq()
              drives.collect:
                case drive if !drive.inGoodHealth => GeneralDriveStatus.BadHealth
                case drive if databaseService.findBySerialNumber[HardDriveModel](drive.itsaID).isLeft => GeneralDriveStatus.NotLogged
                case _ => GeneralDriveStatus.NoIssues
              .distinct
            end issues
            val mem = memory
            val versionID = s"${info.versionID} ${BuildInfo.version.toLowerCase.replaceAll("-snapshot", " ")}"
            info.copy(
              versionID = versionID,
              batteryHealth = battery.map(_.head).map(_.health).getOrElse(BatteryHealth(0.0)),
              amountOfMemory = mem.map(_.toTotalGBString) ?? "0GB",
              numberOfDrives = hardDrives.map(_.size) ?? 0,
              anyDriveIssues = issues,
              amountOfStorage =  hardDrives.map(_.map(_.size).toTotalGBString) ?? "0GB",
              itsaID = itsaID,
              processorDescription = processors.map(_.head).map(_.longDescription)?? "")
        end updatedOption
        val result =
          if updatedOption.isDefined then
            Seq(updatedOption.get).?
          else
            Seq.empty.?
          end if
        end result
        data = data.copy(info = result)
        result ?? Seq()
  end reloadGeneralInfo


  private def checkItsaID()(using databaseService: HLDatabaseService)(using Ox): Either[Throwable, Option[String]] =
    ox.either:
      val id = data.info
        .flatMap(_.headOption)
        .flatMap: info =>
          databaseService.findBySerialNumber[GeneralInfoModel](info.serial).toOption.flatMap(_.itsaID)
          .flatMap(_.split("-E").headOption)
      NotificationCentre.sendItsaIDChangedNotification(id ?? "")
      id
  end checkItsaID


  private def reloadMemory()(using hardwareGrabberService: HardwareGrabberService)(using Ox): Either[Throwable, Seq[MemoryModel]] =
    ox.either:
      val memoryModel: Seq[MemoryModel] =hardwareGrabberService.loadMemory().ok()
      data = data.copy(memory = memoryModel.?)
      memoryModel

  private def reloadHardDrives()(using hardwareGrabberService: HardwareGrabberService)(using Ox): Either[Throwable, Seq[HardDriveModel]] =
    ox.either:
      val result: Option[Seq[HardDriveModel]] = hardwareGrabberService.loadHardDrives().?
      data = data.copy(drives = result)
      result ?? Seq()
  private def reloadProcessors()(using hardwareGrabberService: HardwareGrabberService)(using Ox): Either[Throwable, Seq[ProcessorModel]] =
    ox.either:
      val processors: Seq[ProcessorModel] = hardwareGrabberService.loadProcessors().ok()
      logger.info(s"Loaded processors $processors")
      data = data.copy(processors = processors.?)
      processors

  private def reloadMedia()(using hardwareGrabberService: HardwareGrabberService)(using Ox): Either[Throwable, Any] =
    ox.either:
      val media: Seq[MediaModel] = hardwareGrabberService.loadMedia().ok()
        data = data.copy(media = media.?)
      media

  /**
   * Public standalone endpoint targeted by granular ViewModel ScheduledServices.
   */
  def reloadBattery()(using hardwareGrabberService: HardwareGrabberService)(using Ox): Either[Throwable, Unit] =
    val batteryResult = hardwareGrabberService.loadBatteryStatus()
      data = data.copy(battery = batteryResult.?)
      data.info.map(_.head).foreach: info =>
        val health = batteryResult.map(_.headOption.map(_.health)).map(_.head).getOrElse(BatteryHealth(0.0))
        val result = Seq(info.copy(batteryHealth = health))
        data = data.copy(info = result.?)
    Right(())

  // ==========================================
  // Core Pipeline Orchestrator
  // ==========================================

  /**
   * Orchestrates an expensive, comprehensive load pipeline.
   *
   * @param task UI representation tracking overall task progress.
   * @param currentIndex Initial placement configuration context.
   * @return The updated index tracking progress markers.
   */
  def reload[T <: SimpleTask[R], R](task: T, currentIndex: Double = 0D)(using hardwareGrabberService: HardwareGrabberService, databaseService: HLDatabaseService, totalNumberOfItems: Int = 0)(using Ox): Either[Throwable, Double] =
    val mode = ox.EitherMode[Throwable]()
    ox.supervisedError(mode):
      var index = currentIndex

      def performAsTask(msg: String)(using allowedMode: ProgramMode = "Normal")(body: => Either[Throwable, Any]) =
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

     
      performAsTask("Checking for ITSA ID..."):
        checkItsaID()

      performAsTask("Loading memory..."):
        reloadMemory()

      performAsTask("Loading drives...")(using allowedMode = "Both"):
        reloadHardDrives()

      performAsTask("Loading processors..."):
        reloadProcessors()

      performAsTask("Loading media..."):
        reloadMedia()

      performAsTask("Loading battery..."):
        reloadBattery()

      performAsTask("Loading general info...")(using allowedMode = "Both"):
        reloadGeneralInfo()


      Right(index)
  end reload

  // ==========================================
  // Event-Driven Boundary Configuration Hooks
  // ==========================================

  NotificationCentre.bindToFetchData:
    data |> Right.apply

  NotificationCentre.bindToGetData:
    Option(data)

  NotificationCentre.bindToFetchGlobalValidationContext(globalValidationContext)

  NotificationCentre.bindToUpdateSharedData: newData =>
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

  NotificationCentre.addItsaIDChangedListener: newItsaID =>
    data = data.copy(oldItsaID = data.itsaID, itsaID = newItsaID.?)

end HardwareLoggerApplicationState
