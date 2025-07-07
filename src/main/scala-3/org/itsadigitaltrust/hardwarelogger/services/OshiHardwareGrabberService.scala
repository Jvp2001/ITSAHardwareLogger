package org.itsadigitaltrust.hardwarelogger.services

import org.itsadigitaltrust.common.Maths.*
import org.itsadigitaltrust.common.{NetworkUtils, OSUtils}
import org.itsadigitaltrust.common.Operators.{??, |>}
import org.itsadigitaltrust.common.Types.{Percentage, asPercentage}
import org.itsadigitaltrust.common.processes.lshw.types.CdRom
import org.itsadigitaltrust.common.processes.{Dmidecode, Lsblk, lshw}
import org.itsadigitaltrust.common.types.DataSizeType.{DataSize, DataSizeUnit}
import org.itsadigitaltrust.common.types.FrequencyType.Frequency
import org.itsadigitaltrust.common.types.FrequencyUnit

import org.itsadigitaltrust.hardwarelogger.core.HLProcessConfig
import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.models.*
import org.itsadigitaltrust.hardwarelogger.tasks.{HLTaskGroupBuilder, HLTaskRunner, HardwareGrabberTask}

import org.itsadigitaltrust.hdsentinelreader.HDSentinelReader
import org.itsadigitaltrust.hdsentinelreader.data.HardDiskSummary
import org.scalafx.extras
import oshi.SystemInfo
import scalafx.application.Platform

import scala.jdk.CollectionConverters.*


trait OshiHardwareGrabberService extends HardwareGrabberService:

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


  // serial number was this: S1CTNSAG440003
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
          <Hard_Disk_Serial_Number>S13TJ1CQ404992</Hard_Disk_Serial_Number>
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


  protected def findGeneralInfoByPCSerialNumber(serial: String): Option[String]

  protected def findDriveIdBySerialNumber(serial: String): Option[String]

  protected def findItsaIdBySerialNumber(serial: String): Option[String]

  override def loadGeneralInfo(): Unit =
    extras.onFX:
      val serialNumber = hal.getComputerSystem.getSerialNumber
      val model = hal.getComputerSystem.getModel
      val vendor = hal.getComputerSystem.getManufacturer
      val os = System.getProperty("os.name")
      val id = findItsaIdBySerialNumber(serialNumber) ?? ""
      generalInfo = GeneralInfoModel("itsa-hwlogger", description = /*dmidecode.getKeywordValue("chassis-type")*/ Dmidecode("chassis-type"), model = model, vendor = vendor, serial = /*serialNumber*/ serialNumber, os = os, itsaID = Some(id))


  override def loadHardDrives(): Unit =
    val hdSentinelReader =
      if OSUtils.onLinux then
        HDSentinelReader[HardDiskSummary]()
      else
        HDSentinelReader[HardDiskSummary](xml)
    end hdSentinelReader

    val hardDiskSummaries: Seq[HardDiskSummary] =
      hdSentinelReader.getAllNodesInElementsStartingWith("Physical_Disk_Information_Disk", "Hard_Disk_Summary")
    val lsblk = Lsblk()

    hardDrives = hardDiskSummaries.map: hardDiskSummary =>

      val driveId = findDriveIdBySerialNumber(hardDiskSummary.hardDiskSerialNumber)
      val serialId = findItsaIdBySerialNumber(hal.getComputerSystem.getSerialNumber)
      val id = (driveId, serialId) match
        case (Some(dId), None) => dId
        case (None, Some(sId)) => sId
        case (Some(dId), Some(_)) => dId
        case _ => ""
      val hdType: HardDriveType =
        if OSUtils.onLinux then
          val rota = lsblk(hardDiskSummary.hardDiskDevice).map(_.rota) ?? false
          if rota then "HDD" else "SSD"
        else "SSD"

      val totalSize = hardDiskSummary.totalSize.split(" ")(0).toLongOption ?? 0L
      val size = totalSize / 1000
      val dataSize = DataSize(size, DataSizeUnit.GB)
      System.out.println(s"Drive size: ${dataSize.dbString}")
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

  end loadHardDrives


  override def loadMemory(): Unit =
    memory =
      hal.getMemory.getPhysicalMemory.asScala.map: memory =>
        val size = memory.getCapacity.GiB
        val description = memory.getManufacturer
        MemoryModel(size, description, memory.getMemoryType)
      .toList
  end loadMemory


  override def loadProcessors(): Unit =
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
    processors = List(ProcessorModel(name, frequency, desc, longDesc, pi.getProcessorID, width, cores, threads))
  end loadProcessors


  override def loadMedia(): Unit =
    val cds = org.itsadigitaltrust.common.processes.Lshw.readDisks()
    def toModel(cd: CdRom): MediaModel = MediaModel(cd.description, cd.handle)
    media = cds.map(_.map(_ |> toModel)) ?? Seq()



end OshiHardwareGrabberService

object OshiHardwareGrabberApplicationService extends ServicesModule, OshiHardwareGrabberService:

  override protected def findItsaIdBySerialNumber(serial: String): Option[String] =
    databaseService.findItsaIdBySerialNumber(serial)


  override protected def findDriveIdBySerialNumber(serial: String): Option[String] =
    var string = Option("")


    if ProgramMode.isInNormalMode && NetworkUtils.isConnected then
      string = databaseService.findItsaIdBySerialNumber(serial)
    else
      string = Option(databaseService.findWipingRecord(serial).map(_.itsaID) ?? null)
    string

  override protected def findGeneralInfoByPCSerialNumber(serial: String): Option[String] =
    if !NetworkUtils.isConnected then
      None
    else
      databaseService.findItsaIdBySerialNumber(serial)


  override def load()(finished: () => Unit = () => ()): Unit =
    val taskGroupBuilder = new HLTaskGroupBuilder[HardwareGrabberTask, Unit](HardwareGrabberTask(_))
    ProgramMode.mode match
      case "Normal" =>
        taskGroupBuilder.addAll(
          loadGeneralInfo(),
          loadHardDrives(),
          loadMemory(),
          loadProcessors(),
          loadMedia()
        )
      case "HardDrive" =>
        taskGroupBuilder.add(loadHardDrives())
    end match

    val infoType = if ProgramMode.isInNormalMode then "Hardware" else "Hard Drive"
    taskGroupBuilder.run(s"Getting $infoType Information")(finished)

  end load
end OshiHardwareGrabberApplicationService



