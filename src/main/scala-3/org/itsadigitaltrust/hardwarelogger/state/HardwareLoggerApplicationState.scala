package org.itsadigitaltrust.hardwarelogger.state

import org.itsadigitaltrust.common.Operators.{?, ??, |>}
import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.common.types.*
import org.itsadigitaltrust.hardwarelogger.core.BuildInfo
import org.itsadigitaltrust.hardwarelogger.core.DataTypes.*
import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.models.*
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.*
import org.itsadigitaltrust.hardwarelogger.services.validation.GlobalValidationContext
import org.itsadigitaltrust.hardwarelogger.services.{HLDatabaseService, HardwareGrabberService}
import org.itsadigitaltrust.hardwarelogger.shared.HardwareLoggerSharedData
import org.scalafx.extras.BusyWorker.SimpleTask
import ox.{Fork, Ox}
import ox.either.ok
import scalafx.application.Platform

import java.util.concurrent.atomic.AtomicReference
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

private[state] type Data = HardwareLoggerSharedData

extension (drives: Option[Seq[HardDriveModel]])
  def filterBySerial: Option[Seq[HardDriveModel]] =
    drives.map (_.groupBy (_.serial).map:
      case (serialNumber, duplicates) =>
        duplicates.find(drive => HardDriveModel.hasLoggedItsaID(drive.itsaID)) match
        case Some (driveWithId) => driveWithId
        case _ => duplicates.head
      ).map(_.toSeq)
case class HardwareLoggerApplicationState()(using hardwareGrabberService: HardwareGrabberService, databaseService: HLDatabaseService)
  extends HWLLoggable, RequiresGlobalValidationContext:

  private val dataRef = AtomicReference(HardwareLoggerSharedData())
  private val globalValidationContext: GlobalValidationContext[? <: HLModel] = GlobalValidationContext.globalValidationContext

  private def updateData(func: HardwareLoggerSharedData => HardwareLoggerSharedData): HardwareLoggerSharedData =
    dataRef.updateAndGet(func(_))

  def getData: Data = dataRef.get

  def generalInfo: Option[Seq[GeneralInfoModel]] = getData.info
  def memory: Option[Seq[MemoryModel]] = getData.memory
  def hardDrives: Option[Seq[HardDriveModel]] = getData.drives
  def media: Option[Seq[MediaModel]] = getData.media
  def processors: Option[Seq[ProcessorModel]] = getData.processors
  def itsaID: Option[String] = getData.itsaID
  def oldItsaID: Option[String] = getData.oldItsaID
  def battery: Option[Seq[BatteryStatusModel]] = getData.battery

  def addDrives(hardDriveModels: HardDriveModel*): Unit =
    updateData: current =>
      current.copy(drives = (current.drives ?? Seq()).concat(hardDriveModels).?)


  def updateDriveItsaIDBySerial(serial: Option[String], newItsaID: Option[String]): Unit =
    serial.map: driveSerial =>
      hardDrives.map: drives =>
        val foundDrive = drives.find(_.serial == driveSerial)
        val updatedDrive = foundDrive.map: drive =>
          val newDrive = newItsaID.map: newID =>
            drive.copy(itsaID = newID)
          (drives.indexOf(drive), newDrive)
        val updatedDrives = updatedDrive.map: (index, drive) =>
          drive.map(drives.updated(index, _)) ?? Seq()
        dataRef.updateAndGet: data =>
          val newDrives = updatedDrives
          end newDrives
          data.copy(drives = newDrives.map(_.toSeq))
    .orElse: 
      logger.warn("No drive found with serial number")
      None
  end updateDriveItsaIDBySerial

  // ==========================================
  // Extracted Component Reload Options (Thread-Safe)
  // ==========================================

  private def reloadGeneralInfo()(using HardwareGrabberService, HLDatabaseService)(using Ox): Either[Throwable, Seq[GeneralInfoModel]] =
    ox.either:
      val option: Seq[GeneralInfoModel] = hardwareGrabberService.loadGeneralInfo().ok()
      val updatedOption =
        option.headOption.map: info =>
          val issues: Seq[GeneralDriveStatus] =
            val drives = hardDrives ?? Seq()
            val driveIssues = drives.flatMap: drive =>
              Seq(
                Option.when(!drive.inGoodHealth)(GeneralDriveStatus.BadHealth),
                Option.when(databaseService.findWipingIDBySerialNumber(drive.serial).ok().isEmpty)(GeneralDriveStatus.NotLogged)
              ).flatten
            if driveIssues.isEmpty then Seq(GeneralDriveStatus.NoIssues) else driveIssues.distinct
          end issues
          val mem = memory
          val versionID = s"${info.versionID} ${BuildInfo.version}"
          info.copy(
            model = info.model.split("\\(").headOption.getOrElse(info.model),
            versionID = versionID,
            batteryHealth = battery.map(_.head).map(_.health).getOrElse(BatteryHealth(0.0)),
            amountOfMemory = mem.map(_.toTotalGiBString.replace("GiB", "GB")) ?? "0GB",
            numberOfDrives = hardDrives.map(_.size) ?? 0,
            anyDriveIssues = issues,
            amountOfStorage = hardDrives.map(_.map(_.size).toTotalGBString) ?? "0GB",
            itsaID = itsaID,
            chipType = processors.map(_.head).map(_.name) ?? "")
      end updatedOption
      val result = if updatedOption.isDefined then Seq(updatedOption.get).? else Seq.empty.?
      updateData(_.copy(info = result))
      result ?? Seq()
  end reloadGeneralInfo

  private def checkItsaID()(using HLDatabaseService)(using Ox): Either[Throwable, Option[String]] =
    ox.either:
      val info = getData.info
        .flatMap(_.headOption)
      val id = info
        .flatMap(model =>
          databaseService.findBySerialNumber[GeneralInfoModel](model.serial).toOption.flatMap(_.itsaID)
            .flatMap(_.split("-E").headOption)
        )
      if info.nonEmpty then
        updateData: data =>
          data.copy(info = data.info.map(_.map(_.copy(itsaID = id))))
      NotificationCentre.sendItsaIDChangedNotification(id ?? "")
      id
  end checkItsaID

  private def reloadMemory()(using HardwareGrabberService)(using Ox): Either[Throwable, Seq[MemoryModel]] =
    ox.either:
      val memoryModel: Seq[MemoryModel] = hardwareGrabberService.loadMemory().ok()
      updateData(_.copy(memory = memoryModel.?))
      memoryModel

  private def reloadHardDrives()(using HardwareGrabberService)(using Ox): Either[Throwable, Seq[HardDriveModel]] =
    ox.either:
      val physicalDrives: Seq[HardDriveModel] = hardwareGrabberService.loadHardDrives().? ?? Seq()
      val result = physicalDrives.map: drive =>
        val loggedID =
          if ProgramMode.isInHardDriveMode then
            databaseService.findWipingIDBySerialNumber(drive.serial).ok()
          else
            databaseService.findDiskIDBySerialNumber(drive.serial).ok()
        drive.copy(itsaID = loggedID.getOrElse("NOT LOGGED"))
      updateData: data =>
        val newDrives = result ++ data.drives ?? Seq()
        data.copy(drives = newDrives.distinct.?.filterBySerial)
      result

  private def reloadProcessors()(using HardwareGrabberService)(using Ox): Either[Throwable, Seq[ProcessorModel]] =
    ox.either:
      val processors: Seq[ProcessorModel] = hardwareGrabberService.loadProcessors().ok()
      updateData(_.copy(processors = processors.?))
      processors

  private def reloadMedia()(using HardwareGrabberService)(using Ox): Either[Throwable, Any] =
    ox.either:
      val media: Seq[MediaModel] = hardwareGrabberService.loadMedia().ok()
      updateData(_.copy(media = media.?))
      media

  def reloadBattery()(using HardwareGrabberService)(using Ox): Either[Throwable, Unit] =
    val batteryResult = hardwareGrabberService.loadBatteryStatus()
    updateData(_.copy(battery = batteryResult.?))
    getData.info.flatMap(_.headOption).foreach: info =>
      val health = batteryResult.map(_.headOption.map(_.health)).map(_.head).getOrElse(BatteryHealth(0.0))
      val result = Seq(info.copy(batteryHealth = health))
      updateData(_.copy(info = result.?))
    Right(())

  // ==========================================
  // Core Parallel Pipeline Execution Ring
  // ==========================================

  private def runPipeline(task: SimpleTask[?])(using Ox): Either[Throwable, Unit] =
    val mode = ox.EitherMode[Throwable]()
    ox.supervisedError(mode):
      val totalNumberOfItems = 8
      var index = 0D

      val forkBuffer = ArrayBuffer[Fork[Either[Throwable, Any]]]()

      def scheduleAsTask(msg: String)(using allowedMode: ProgramMode = "Normal")(body: => Either[Throwable, Any]): Unit =
        if ProgramMode.mode == allowedMode || allowedMode == "Both" then
          val newFork: Fork[Either[Throwable, Any]] = ox.forkUserError:
              ox.either:
                logger.info(msg)
                body
          forkBuffer += newFork
      end scheduleAsTask

      def performSequentialTask(msg: String)(body: => Either[Throwable, Any]): Either[Throwable, Any] =
        ox.either:
          Platform.runLater(task.message() = msg)
          body.ok()
          index += 1
          Platform.runLater(task.progress() = index / totalNumberOfItems)

      // 1. Core State Hardware processing runs concurrently first
      scheduleAsTask("Loading memory...")(reloadMemory())
      scheduleAsTask("Loading drives...")(using allowedMode = "Both")(reloadHardDrives())
      scheduleAsTask("Loading processors...")(reloadProcessors())
      scheduleAsTask("Loading media...")(reloadMedia())
      scheduleAsTask("Loading battery...")(reloadBattery())

      // 2. Synchronise background hardware data tasks
      forkBuffer.foreach: fork =>
        ox.either:
          fork.join().ok()
          index += 1
          Platform.runLater(task.progress() = index / totalNumberOfItems)

      // 3. Process structural schema layout configurations sequentially
      performSequentialTask("Loading general info..."):
        reloadGeneralInfo()

      performSequentialTask("Checking for ITSA ID..."):
        checkItsaID()

      // ====================================================
      // PHASE 2: Tell all ViewModels to reload now that data is safe
      // ====================================================
      NotificationCentre.publish(ReloadViewModelsEvent)

      Right(())
  end runPipeline

  // ==========================================
  // Event-Driven Boundary Configuration Hooks
  // ==========================================

  // Registers this component frame onto the central event architecture network channels
  NotificationCentre.subscribe[StartReloadEvent]: event =>
    // Spawns a structured background concurrency scope ring to execute the hardware loads safely
    ox.supervised:
      runPipeline(event.task)

  NotificationCentre.registerDataProvider(getData)
  NotificationCentre.registerValidationContextProvider(globalValidationContext)

  NotificationCentre.subscribe[ItsaIDChangedEvent]: event =>
    updateData:current =>
      current.copy(oldItsaID = current.itsaID, itsaID = event.itsaID.?)


end HardwareLoggerApplicationState
