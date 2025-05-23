package org.itsadigitaltrust.hdsentinelreader

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.`type`.TypeFactory
import com.fasterxml.jackson.dataformat.xml.{JacksonXmlModule, XmlMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import java.io.File
import scala.io.Source
import scala.util.Using
import scala.xml.{Document, Elem}
import org.itsadigitaltrust.common.*
import data.HardDiskSummary
import org.itsadigitaltrust.common.processes.{ProcessConfig, sudo}
import xml.*

import scala.reflect.ClassTag


class HDSentinelReader:

  import scala.compiletime.constValue

  private val xmlParser = new XMLParser

  inline def read(xml: String): Unit =
    xmlParser.read(xml)

  inline def read(xml: Elem): Unit =
    xmlParser.read(xml)

  inline def \[T : ClassTag](name: String): T =
    xmlParser \ name

  def getAllNodesInElementsStartingWith[T : ClassTag](startingWith: HDSentinelReader.ElementName, childName: HDSentinelReader.ElementName): Seq[T] =
    given xmlMapper:XmlMapper = xmlParser.xmlMapper
    val nodes = xmlParser.getAllNodesStartingWith(startingWith)
    nodes.map: node =>
      node \\> childName

end HDSentinelReader

object HDSentinelReader:
  type ElementName = "Hard_Disk_Sentinel" | "General_Information" | "Application_Information" | "Installed_version" | "Current_Date_And_Time" | "Report_Creation_Time" | "Computer_Information" | "Computer_Name" | "MAC_Address" | "System_Information" | "OS_Version" | "Process_ID" | "Uptime" | "Physical_Disk_Information_Disk" | "Hard_Disk_Summary" | "Hard_Disk_Number" | "Hard_Disk_Device" | "Interface" | "Hard_Disk_Model_ID" | "Firmware_Revision" | "Hard_Disk_Serial_Number" | "Total_Size" | "Current_Temperature" | "Maximum_temperature_during_entire_lifespan" | "Lifetime_writes" | "Health" | "Performance" | "Description" | "Tip" | "Properties" | "NVMe_Standard_Version" | "PCI_Vendor_ID_VID" | "PCI_Subsystem_Vendor_ID_SSVID" | "IEEE_OUI_Identifier" | "Recommended_Arbitration_Burst_RAB" | "Multi_Interface_Capabilities" | "Maximum_Data_Transfer_Size" | "Abort_Command_Limit" | "Asynchronous_Event_Request_Limit" | "Number_FW_Slots_Support" | "Maximum_Error_Log_Page_Entries" | "Total_Number_Of_Power_States" | "Admin_Vendor_Specific_CMD_Format" | "Submission_Queue_Entry_Size" | "Completion_Queue_Entry_Size" | "Number_of_Namespaces" | "Stripe_Size" | "Maximum_Power_mW" | "NVMe_Features" | "Doorbell_Buffer_Config" | "Virtualization_Management" | "NVMe_MI_Send_Receive" | "Directives" | "Device_Self_test" | "Namespace_Management" | "Firmware_Activate_Download" | "Format_NVM" | "Security_Send_Receive" | "Firmware_Activation_Without_Reset" | "First_Firmware_Slot_Read_Only" | "Command_Effects_Log_Page" | "SMART_Information_Per_Namespace" | "Reservations" | "Save_Select_Fields" | "Write_Zeroes" | "Dataset_Management_Command" | "Write_Uncorrectable_Command" | "Compare_Command" | "Compare_and_Write_Fused_Operation" | "Cryptographic_Erase" | "Secure_Erase_All_Namespaces" | "Format_All_Namespaces" | "Volatile_Write_Cache_Present" | "Autonomous_Power_State_Transitions" | "Atomic_Compare_And_Write_Unit" | "Scatter_Gather_List_SGL" | "Host_Controlled_Thermal_Management" | "Sanitize_Overwrite" | "Sanitize_Block_Erase" | "Sanitize_Crypto_Erase" | "S.M.A.R.T." | "Attribute" | "ATA_Information" | "Hard_Disk_Cylinders" | "Hard_Disk_Heads" | "Hard_Disk_Sectors" | "ATA_Revision" | "Total_Sectors" | "Bytes_Per_Sector" | "Buffer_Size" | "Multiple_Sectors" | "Error_Correction_Bytes" | "Unformatted_Capacity" | "Maximum_PIO_Mode" | "Maximum_Multiword_DMA_Mode" | "Maximum_UDMA_Mode" | "Active_UDMA_Mode" | "Minimum_multiword_DMA_Transfer_Time" | "Recommended_Multiword_DMA_Transfer_Time" | "Minimum_PIO_Transfer_Time_Without_IORDY" | "Minimum_PIO_Transfer_Time_With_IORDY" | "ATA_Control_Byte" | "ATA_Checksum_Value" | "Acoustic_Management_Configuration" | "Acoustic_Management" | "Disabled" | "Current_Acoustic_Level" | "Recommended_Acoustic_Level" | "ATA_Features" | "Read_Ahead_Buffer" | "DMA" | "Supported" | "Ultra_DMA" | "Power_Management" | "Write_Cache" | "Host_Protected_Area" | "HPA_Security_Extensions" | "Advanced_Power_Management" | "Enabled" | "Advanced_Power_Management_Level" | "Extended_Power_Management" | "Power_Up_In_Standby" | "X48_bit_LBA_Addressing" | "Device_Configuration_Overlay" | "IORDY_Support" | "Read_Write_DMA_Queue" | "NOP_Command" | "Trusted_Computing" | "X64_bit_World_Wide_ID" | "Streaming" | "Media_Card_Pass_Through" | "General_Purpose_Logging" | "Error_Logging" | "CFA_Feature_Set" | "CFast_Device" | "Long_Physical_Sectors_1" | "Long_Logical_Sectors" | "Write_Read_Verify" | "NV_Cache_Feature" | "NV_Cache_Power_Mode" | "NV_Cache_Size" | "Free_fall_Control" | "Free_fall_Control_Sensitivity" | "Service_Interrupt" | "IDLE_IMMEDIATE_command_with_UNLOAD_feature" | "SCT_Command_Transport" | "SCT_Error_Recovery_Control" | "Nominal_Media_Rotation_Rate" | "Zoned_Capabilities" | "SSD_Features" | "Data_Set_Management" | "TRIM_Command" | "Deterministic_Read_After_TRIM" | "Read_Zeroes_After_TRIM" | "Security_Mode" | "Security_Erase" | "Security_Erase_Time" | "Security_Enhanced_Erase_Feature" | "Security_Enhanced_Erase_Time" | "Security_Enabled" | "No" | "Security_Locked" | "Security_Frozen" | "Security_Counter_Expired" | "Security"
  def apply[T: ClassTag]()(using config: ProcessConfig): HDSentinelReader =
    val reader = new HDSentinelReader
    if OSUtils.onLinux then
      val xml = sudo"HDSentinel -r -xml -dump"
      println(s"XML: $xml")
      reader.read(xml)
    reader

  inline def apply[T: ClassTag](elem: Elem): HDSentinelReader =
    val reader = new HDSentinelReader
    reader.read(elem)
    reader

end HDSentinelReader

