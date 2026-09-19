package org.itsadigitaltrust.hardwarelogger.services

import org.itsadigitaltrust.common.Maths.*
import org.itsadigitaltrust.common.Operators.*
import org.itsadigitaltrust.common.processes.lshw.types.CdRom
import org.itsadigitaltrust.common.processes.{Dmidecode, Lsblk, SudoError}
import org.itsadigitaltrust.common.types.DataSizeType.{DataSize, DataSizeUnit}
import org.itsadigitaltrust.common.types.FrequencyType.Frequency
import org.itsadigitaltrust.common.types.FrequencyUnit
import org.itsadigitaltrust.common.{NetworkUtils, OSUtils}
import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.models.HLModel.{BatteryStatusModel, GeneralInfoModel, HardDriveConnectionType, HardDriveModel, HardDriveType, MediaModel, MemoryModel, ProcessorModel}
import org.itsadigitaltrust.hdsentinelreader.HDSentinelReader
import org.itsadigitaltrust.hdsentinelreader.data.HardDiskSummary
import org.itsadigitaltrust.common.asPercentage
import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.common.processes.lshw.Lshw.LshwError
import org.itsadigitaltrust.common.processes.tlp.{TlpBatteryStatus, TlpStat}
import org.itsadigitaltrust.hardwarelogger.HardwareLoggerApplication.databaseService
import org.itsadigitaltrust.hardwarelogger.models.HLModel
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.RequiresItsaID
import oshi.SystemInfo
import ox.{EitherMode, Ox}
import ox.either.{fail, ok, orThrow}

import scala.jdk.CollectionConverters.*
import scala.util.NotGiven

trait OshiHardwareGrabberService(using databaseService: HLDatabaseService) extends HardwareGrabberService, RequiresItsaID, HWLLoggable:

  private val systemInfo = new SystemInfo
  private val hal = systemInfo.getHardware

  given Conversion[String, HardDriveConnectionType] = s =>
    if s.startsWith("S-ATA") then
      HardDriveConnectionType.SATA
    else if s.contains("IDE") then
      HardDriveConnectionType.PATA
    else if s.contains("SCSI") then
      HardDriveConnectionType.SCSI
    else if s.toLowerCase.contains("nvme") then
      HardDriveConnectionType.NVME
    else
      HardDriveConnectionType.UNKNOWN
  end given





  override def loadGeneralInfo()(using ox.Ox): Result[GeneralInfoModel] =
    import org.itsadigitaltrust.hardwarelogger.core.Givens.given
    ox.either:
      try
        val serialNumber = hal.getComputerSystem.getSerialNumber
        val model = hal.getComputerSystem.getModel
        val vendor = hal.getComputerSystem.getManufacturer
        val os = System.getProperty("os.name")
        // Without the explicit type argument `M` is minimised to `Nothing`, no mapper can be
        // summoned and the lookup can never succeed.
        val description = Dmidecode("chassis-type").orThrow ?? "Unknown"
        val info = GeneralInfoModel("itsa-hwlogger", description = description,
          model = model,
          vendor = vendor,
          serial = serialNumber,
          os = os,
        )
        Seq(info)
      catch
        case e: Exception =>
          LoadError(e).fail()
  end loadGeneralInfo


  override def loadHardDrives()(using ox.Ox): Result[HardDriveModel] =
    import org.itsadigitaltrust.hardwarelogger.core.Givens.given
    ox.either:
      try
        val hdSentinelReader =
          HDSentinelReader[HardDiskSummary]().left.map(LoadError.apply).ok()
        end hdSentinelReader

        val hardDiskSummaries: Seq[HardDiskSummary] =
          hdSentinelReader.getAllNodesInElementsStartingWith("Physical_Disk_Information_Disk", "Hard_Disk_Summary")
        val lsblk = Lsblk()


        hardDiskSummaries.map: hardDiskSummary =>

          val id: String = itsaID


          val hdType: HardDriveType =
            if OSUtils.onLinux then
              val rotaOpt = lsblk(hardDiskSummary.hardDiskDevice).map(_.rota)
              rotaOpt match
                case Some(true)  => "HDD"
                case Some(false) => "SSD"
                case None        => "UNKNOWN"
            else
              "UNKNOWN"

          def sizeStringToBytes(s: String): Long =
            val parts = s.trim.split("\\s+").toList
            val numberOpt: Option[Long] =
              parts.headOption
                .map(_.replaceAll("[^0-9]", "")) // keep digits only
                .filter(_.nonEmpty)
                .flatMap(_.toLongOption)

            val unitOpt = parts.drop(1).headOption.map(_.toUpperCase)

            (numberOpt, unitOpt) match
              case (Some(n), Some(u)) if u.startsWith("TB") => n * 1_000_000_000_000L
              case (Some(n), Some(u)) if u.startsWith("GB") => n * 1_000_000_000L
              case (Some(n), Some(u)) if u.startsWith("MB") => n * 1_000_000L
              case (Some(n), Some(u)) if u.startsWith("KB") => n * 1_000L
              case (Some(n), Some(u)) if u.startsWith("B")  => n
              case (Some(n), None)                           => n
              case _                                         => 0L
          end sizeStringToBytes

          val totalBytes: Long = sizeStringToBytes(hardDiskSummary.totalSize)
          val dataSize = DataSize(totalBytes.toDouble, DataSizeUnit.GB) // value treated as bytes (converted to GB)
          logger.info(s"Drive size: ${dataSize.dbString}")

          val drive = HardDriveModel(
            hardDiskSummary.health.asPercentage,
            hardDiskSummary.performance.asPercentage,
            dataSize,
            hardDiskSummary.hardDiskModelId,
            hardDiskSummary.hardDiskSerialNumber,
            itsaID = id,
            connectionType = hardDiskSummary.interfaceType,
            `type` = hdType,
            actions = hardDiskSummary.tip,
            description = hardDiskSummary.description,
            powerOnTime = hardDiskSummary.powerOnTime,
            estimatedRemainingLifetime = hardDiskSummary.estimatedRemainingLifetime
          )
          drive
      catch
        case e: Exception =>
          logger.trace("Error while loading hard drive info", e)
          LoadError(e.getMessage).fail()

  end loadHardDrives



  override def loadMemory()(using ox.Ox): Result[MemoryModel] =
    ox.either:
      hal.getMemory.getPhysicalMemory.asScala.map: memory =>
        val size = memory.getCapacity.GiB
        val description = memory.getManufacturer
        MemoryModel(size, description, memory.getMemoryType)
      .toList
  end loadMemory


  override def loadProcessors()(using ox.Ox): Result[ProcessorModel] =
    ox.either:
      try
        val proc = hal.getProcessor
        val pi = proc.getProcessorIdentifier
        val name = pi.getName
        val desc = pi.getFamily
        val width = if pi.isCpu64bit then 64 else 32
        val longDesc = pi.getVendor
        val cores = hal.getProcessor.getPhysicalProcessorCount
        val threads = proc.getLogicalProcessorCount
        val freq = hal.getProcessor.getCurrentFreq
        val avgFreq = freq.sum / freq.length
        val maxFreq = proc.getMaxFreq
        val freqToUse = if maxFreq != -1 then maxFreq else avgFreq
        val frequency = Frequency(freqToUse, FrequencyUnit.GHz)
        Seq(ProcessorModel(name, frequency, desc, longDesc, pi.getProcessorID, width, cores, threads))
      catch
        case e: Exception =>
          logger.trace("Error while loading hard drive info", e)
          LoadError(e.getMessage).fail()
  end loadProcessors


  // ... existing code ...

  override def loadMedia()(using ox.Ox): Result[MediaModel] =
    ox.either:
      try

        org.itsadigitaltrust.common.processes.Lshw.readDisks() match
          case Right(cds) =>
            def toModel(cd: CdRom): MediaModel = MediaModel(cd.description, cd.handle)

            cds.map(_ |> toModel)

          case Left(error) =>
            LoadError(error.toString).fail()
      catch
        case e: Exception =>
          logger.trace("Error while loading media info", e)
          LoadError(e).fail()
  
  end loadMedia

  override def loadBatteryStatus()(using Ox): Result[BatteryStatusModel] =
    ox.either:
      TlpStat.getBatteryStatus.map: status =>
        Seq(BatteryStatusModel(status.capacity, status.charge, status.status, status.cycleCount, status.voltage))
      ?? LoadError("Failed to get battery status!").fail()
end OshiHardwareGrabberService

object OshiHardwareGrabberApplicationService extends  OshiHardwareGrabberService:
  private[services] lazy val instance = new OshiHardwareGrabberService() {}
  
  




end OshiHardwareGrabberApplicationService



