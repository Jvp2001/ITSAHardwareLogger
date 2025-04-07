package org.itsadigitaltrust.hardwarelogger.services

import org.itsadigitaltrust.common.Maths.*
import org.itsadigitaltrust.common.Types.{Percentage, asPercentage}
import org.itsadigitaltrust.hardwarelogger.models.*
import org.itsadigitaltrust.hdsentinelreader.HDSentinelReader
import org.itsadigitaltrust.hdsentinelreader.Types.XMLFile
import org.itsadigitaltrust.hdsentinelreader.data.HardDiskSummary
import oshi.SystemInfo
import ox.{fork, supervised}

import scala.jdk.CollectionConverters.*


object OshiHardwareGrabberService extends HardwareGrabberService, ServicesModule:

  import org.itsadigitaltrust.hardwarelogger.models.HardDriveConnectionType.*

  private val systemInfo = new SystemInfo
  private val hal = systemInfo.getHardware
  private val xml = <Hard_Disk_Summary>
    <Hard_Disk_Number>0</Hard_Disk_Number>
    <Interface>S-ATA Gen3, 6 Gbps</Interface>
    <Disk_Controller>����������� ���������� Disk controller</Disk_Controller>
    <Disk_Location>Channel 1, Target 0, Lun 0, Device: 0</Disk_Location>
    <Hard_Disk_Model_ID>SSD Model</Hard_Disk_Model_ID>
    <Firmware_Revision>1234567890</Firmware_Revision>
    <Hard_Disk_Serial_Number>S1CTNSAG440003</Hard_Disk_Serial_Number>
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
    <Health>93 %</Health>
    <Performance>100 %</Performance>
    <Description>The status of the solid state disk is PERFECT. Problematic or weak sectors were not found. The TRIM feature of the SSD is supported and enabled for optimal performance. The health is determined by SSD specific S.M.A.R.T. attribute(s): #177 Wear Leveling Count</Description>
    <Tip>No actions needed.</Tip>
  </Hard_Disk_Summary>


//  if System.getProperty("os.name").toLowerCase.contains("linux") then
//      HDSentinelReader("password", XMLFile("report.xml"))
//    else
//      HDSentinelReader(xml)

  override def load(): Unit =
    supervised:
      fork(loadGeneralInfo())
      fork(loadHardDrives())
      fork(loadMemory())
      fork(loadProcessors())
      fork(loadMedia())
    .join()
    notificationCentre.publish(NotificationChannel.Reload)

  override def loadGeneralInfo(): Unit =
    val serialNumber = hal.getComputerSystem.getSerialNumber
    val model = hal.getComputerSystem.getModel
    val vendor = hal.getComputerSystem.getManufacturer
    val os = System.getProperty("os.name")
    generalInfo = GeneralInfoModel("Need to add", "Need to add", model, vendor, serialNumber, os)

  //TODO: Implement
  override def loadHardDrives(): Unit =

    val onLinux = System.getProperty("os.name").toLowerCase.contains("linux")
    val hdSentinelReader =
      if onLinux then
        HDSentinelReader("password", XMLFile("report.xml"))
      else
        HDSentinelReader(xml)
    val hardDiskSummaries: Seq[HardDiskSummary] =
      if onLinux then
        hdSentinelReader.getAllNodesInElementsStartingWith("Physical_Disk_Information_Disk", "Hard_Disk_Summary")
      else
        import HardDiskSummary.given
        Seq(hdSentinelReader \ "Hard_Disk_Summary")
    hardDrives = hardDiskSummaries.map: hardDiskSummary =>
      println(s"Hard disk summary: $hardDiskSummary")

      val drive = HardDriveModel(
        hardDiskSummary.health.asPercentage,
        hardDiskSummary.performance.asPercentage,
        hardDiskSummary.totalSize.replace(" MB", "").toInt.GiB,
        hardDiskSummary.hardDiskModelId,
        hardDiskSummary.hardDiskSerialNumber,
        SATA,
        `type` = "SSD",
        actions = hardDiskSummary.tip,
        description = hardDiskSummary.description,
        powerOnTime = hardDiskSummary.powerOnTime,
        estimatedRemainingLifetime = hardDiskSummary.estimatedRemainingLifetime
      )
      println(s"Hard drive: $drive")
      drive


  //    hardDrives = hal.getDiskStores.asScala.map: disk =>
  //      val name = disk.getName
  //      val size = disk.getSize
  //      val model = disk.getModel
  //      HardDriveModel(99.percent, 100.percent, size.GiB, model, disk.getSerial, NVME, isSSD = true)
  //    .toList


  override def loadMemory(): Unit =
    memory = hal.getMemory.getPhysicalMemory.asScala.map: memory =>
      val size = memory.getCapacity.MiB
      val description = memory.getManufacturer
      MemoryModel(size, description)
    .toList


  override def loadProcessors(): Unit =

    val pi = hal.getProcessor.getProcessorIdentifier
    val name = pi.getName
    val desc = pi.getFamily
    val width = if pi.isCpu64bit then 64 else 32
    val longDesc = pi.getVendor
    val cores = hal.getProcessor.getPhysicalProcessorCount
    val freq = hal.getProcessor.getCurrentFreq
    processors = List(ProcessorModel(name, freq.sum / freq.length, desc, longDesc, pi.getProcessorID, width, cores))


  override def loadMedia(): Unit =
    ()
  
