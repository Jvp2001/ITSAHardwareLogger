package org.itsadigitaltrust.hdsentinelreader

import org.itsadigitaltrust.hardwarelogger.models.{HardDriveModel, HardDriveConnectionType}
import org.itsadigitaltrust.hdsentinelreader.data.HardDiskSummary
import org.scalatest.funsuite.AnyFunSuite
import org.itsadigitaltrust.common.*


import scala.xml.XML

class HDSentinelReaderTests extends AnyFunSuite:

  val xml = <HDSentinel>
    <Hard_Disk_Summary>
      <Hard_Disk_Number>0</Hard_Disk_Number>
      <Interface>S-ATA Gen3, 6 Gbps</Interface>
      <Disk_Controller>����������� ���������� Disk controller</Disk_Controller>
      <Disk_Location>Channel 1, Target 0, Lun 0, Device: 0</Disk_Location>
      <Hard_Disk_Model_ID>SSD Model</Hard_Disk_Model_ID>
      <Firmware_Revision>1234567890</Firmware_Revision>
      <Hard_Disk_Serial_Number>1234567890</Hard_Disk_Serial_Number>
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
  </HDSentinel>
  test("Get XML output"):
    val reader = /*if System.getProperty("os.name").toLowerCase.contains("linux") then
      HDSentinelReader[HardDriveModel]("password")
    else*/
      HDSentinelReader[HardDiskSummary](xml)
    val hardDiskSummary: HardDiskSummary = reader \ "Hard_Disk_Summary"
    System.out.println(s"HardDiskSummary\n===================\n$hardDiskSummary\n===================")
    val driveModel = new HardDriveModel(
      hardDiskSummary.health.asPercentage,
      hardDiskSummary.performance.asPercentage,
      hardDiskSummary.totalSize.split(" ").head.toInt.GiB,
      hardDiskSummary.hardDiskModelId,
      hardDiskSummary.hardDiskSerialNumber,
      if hardDiskSummary.interfaceType.startsWith("S-ATA") then
        HardDriveConnectionType.SATA
      else
        HardDriveConnectionType.NVME,
      `type` = "SSD"
    )
    assert(driveModel ne null)

    //  reader.xmlParser \ "Hard_Disk_Summary" match
    //   case Some(hardDiskSummary: HardDiskSummary) =>
    //     System.out.println(hardDiskSummary)
    //   case None =>
    //     System.out.println("No data found")
    //     assert(false)
    //   assert(true)
