package org.itsadigitaltrust.hardwarelogger.services

import org.itsadigitaltrust.common.Maths.*
import org.itsadigitaltrust.common.Operators.{??, *}
import org.itsadigitaltrust.common.processes.lshw.types.CdRom
import org.itsadigitaltrust.common.processes.{Dmidecode, Lsblk, SudoError}
import org.itsadigitaltrust.common.types.DataSizeType.{DataSize, DataSizeUnit}
import org.itsadigitaltrust.common.types.FrequencyType.Frequency
import org.itsadigitaltrust.common.types.FrequencyUnit
import org.itsadigitaltrust.common.{NetworkUtils, OSUtils}
import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.models.HLModel.{GeneralInfoModel, HardDriveConnectionType, HardDriveModel, HardDriveType, MediaModel, MemoryModel, ProcessorModel}
import org.itsadigitaltrust.hdsentinelreader.HDSentinalReader
import org.itsadigitaltrust.hdsentinelreader.data.HardDiskSummary
import org.itsadigitaltrust.common.asPercentage
import org.itsadigitaltrust.common.processes.lshw.Lshw.LshwError
import org.itsadigitaltrust.hardwarelogger.HardwareLoggerApplication.databaseService
import org.itsadigitaltrust.hardwarelogger.core.Givens.HLProcessConfig
import oshi.SystemInfo
import ox.EitherMode
import ox.either.{fail, ok, orThrow}

import scala.jdk.CollectionConverters.*
import scala.util.NotGiven

extension (either: Either[HLDatabaseServiceException, String])
  def toOption: Option[String] =
    either match
      case Right(s) => Option(s)
      case _        => None
trait OshiHardwareGrabberService(using databaseService: HLDatabaseService) extends HardwareGrabberService:

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


  // serial number was this: S1CTNSAG440003, then this: S13TJ1CQ404992
  private val xml =
    <Hard_Disk_Sentinal>
      <Physical_Disk_Information_Disk_0>
        <Hard_Disk_Summary>
          <Hard_Disk_Number>0</Hard_Disk_Number>
          <Interface>S-ATA Gen3, 6 Gbps</Interface>
          <Disk_Controller>����������� ���������� Disk controller</Disk_Controller>
          <Disk_Location>Channel 1, Target 0, Lun 0, Device: 0</Disk_Location>
          <Hard_Disk_Model_ID>SSD Model</Hard_Disk_Model_ID>
          <Firmware_Revision>1234567890</Firmware_Revision>
          <Hard_Disk_Serial_Number>Z6EQKBN7</Hard_Disk_Serial_Number>
          <SSD_Controller>SSD Controller</SSD_Controller>
          <Total_Size>12345 MB</Total_Size>
          <Power_State>Active</Power_State>
          <Logical_Drive_s>C: [VOLUME1] D: [VOLUME2]</Logical_Drive_s>
          <Current_Temperature>38 �C</Current_Temperature>
          <Maximum_Temperature_ever_measured>49 �C, 17.07.1970 22:28:37</Maximum_Temperature_ever_measured>
          <Minimum_Temperature_ever_measured>16 �C, 17.11.1970 14:42:12</Minimum_Temperature_ever_measured>
          <Daily_Average>38.80 �C</Daily_Average>
          <Daily_Maximum>40 �C</Daily_Maximum>
          <Power_on_time>1593 days, 6 hours</Power_on_time>
          <Estimated_remaining_lifetime>200 days</Estimated_remaining_lifetime>
          <Health>49%</Health>
          <Performance>100 %</Performance>
          <Description>The status of the solid state disk is PERFECT. Problematic or weak sectors were not found. The TRIM feature of the SSD is supported and enabled for optimal performance. The health is determined by SSD specific S.M.A.R.T. attribute(s): #177 Wear Leveling Count</Description>
          <Tip>No actions needed.</Tip>
        </Hard_Disk_Summary>
      </Physical_Disk_Information_Disk_0>
      <Physical_Disk_Information_Disk_1>
        <Hard_Disk_Summary>
          <Hard_Disk_Number>0</Hard_Disk_Number>
          <Interface>S-ATA Gen3, 6 Gbps</Interface>
          <Disk_Controller>����������� ���������� Disk controller</Disk_Controller>
          <Disk_Location>Channel 1, Target 0, Lun 0, Device: 0</Disk_Location>
          <Hard_Disk_Model_ID>SSD Model</Hard_Disk_Model_ID>
          <Firmware_Revision>1234567890</Firmware_Revision>
          <Hard_Disk_Serial_Number>S13TJ1CQ404993</Hard_Disk_Serial_Number>
          <SSD_Controller>SSD Controller</SSD_Controller>
          <Total_Size>12345 MB</Total_Size>
          <Power_State>Active</Power_State>
          <Logical_Drive_s>C: [VOLUME1] D: [VOLUME2]</Logical_Drive_s>
          <Current_Temperature>38 °C</Current_Temperature>
          <Maximum_Temperature_ever_measured>49 �C, 17.07.1970 22:28:37</Maximum_Temperature_ever_measured>
          <Minimum_Temperature_ever_measured>16 �C, 17.11.1970 14:42:12</Minimum_Temperature_ever_measured>
          <Daily_Average>38.80 °C</Daily_Average>
          <Daily_Maximum>40 °C</Daily_Maximum>
          <Power_on_time>1593 days, 6 hours</Power_on_time>
          <Estimated_remaining_lifetime>200 days</Estimated_remaining_lifetime>
          <Health>93 %</Health>
          <Performance>100 %</Performance>
          <Description>The status of the solid state disk is PERFECT. Problematic or weak sectors were not found. The TRIM feature of the SSD is supported and enabled for optimal performance. The health is determined by SSD specific S.M.A.R.T. attribute(s): #177 Wear Leveling Count</Description>
          <Tip>No actions needed.</Tip>
        </Hard_Disk_Summary>
      </Physical_Disk_Information_Disk_1>
    </Hard_Disk_Sentinal>





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
        val id = databaseService.findBySerialNumber[GeneralInfoModel](serialNumber).toOption.flatMap(_.itsaID)
        val description = Dmidecode("chassis-type").orThrow ?? "Unknown"
        val info = GeneralInfoModel("itsa-hwlogger", description = description,
          model = model,
          vendor = vendor,
          serial = serialNumber,
          os = os,
          itsaID = id)
        Seq(info)
      catch
        case e: Exception =>
          logger.trace("Error while loading hardware info", e)
          LoadError(e).fail()
  end loadGeneralInfo


  override def loadHardDrives()(using ox.Ox): Result[HardDriveModel] =
    import org.itsadigitaltrust.hardwarelogger.core.Givens.given
    ox.either:
      try
        val hdSentinelReader =
          if OSUtils.onLinux then
            HDSentinalReader[HardDiskSummary]().left.map(LoadError.apply).ok()
          else
            HDSentinalReader[HardDiskSummary](xml)
        end hdSentinelReader

        val hardDiskSummaries: Seq[HardDiskSummary] =
          hdSentinelReader.getAllNodesInElementsStartingWith("Physical_Disk_Information_Disk", "Hard_Disk_Summary")
        val lsblk = Lsblk()


        hardDiskSummaries.map: hardDiskSummary =>
          
          val driveId: String =
            val drive = databaseService.findBySerialNumber[HardDriveModel](hardDiskSummary.hardDiskSerialNumber)
            drive.map(_.itsaID) ?? ""

          val pcItsaId: String =
            databaseService.findBySerialNumber[GeneralInfoModel](hal.getComputerSystem.getSerialNumber)
              .toOption.flatMap(_.itsaID) ?? ""


          val id: String =
            driveId ?? pcItsaId ?? ""


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
  

  // ... existing code ...
  

end OshiHardwareGrabberService

object OshiHardwareGrabberApplicationService extends  OshiHardwareGrabberService:
  private[services] lazy val instance = new OshiHardwareGrabberService() {}
  
  




end OshiHardwareGrabberApplicationService



