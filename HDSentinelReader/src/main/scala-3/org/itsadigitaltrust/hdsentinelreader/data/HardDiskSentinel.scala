package org.itsadigitaltrust.hdsentinelreader.data

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper

case class HardDiskSentinel(
                             General_Information: GeneralInformation
                           )

case class GeneralInformation(
                               Application_Information: ApplicationInformation,
                               Computer_Information: ComputerInformation,
                               System_Information: SystemInformation
                             )

case class ApplicationInformation(
                                   Installed_version: String,
                                   Current_Date_And_Time: String,
                                   Report_Creation_Time: String
                                 )

case class ComputerInformation(
                                Computer_Name: String,
                                MAC_Address: String
                              )

case class SystemInformation(
                              OS_Version: String,
                              Process_ID: String,
                              Uptime: String
                            )

case class PhysicalDiskInformationDisk0(
                                         Hard_Disk_Summary: HardDiskSummary,
                                         Properties: Properties,
                                         SCSI_Information: SCSIInformation
                                       )

case class HardDiskSummary(
                            Hard_Disk_Number: String,
                            Hard_Disk_Device: String,
                            Interface: String,
                            Hard_Disk_Model_ID: String,
                            Firmware_Revision: String,
                            Hard_Disk_Serial_Number: String,
                            Total_Size: String,
                            Current_Temperature: String,
                            Maximum_temperature_during_entire_lifespan: String,
                            Health: String,
                            Performance: String
                          )

case class Properties(
                       Vendor_Information: String,
                       Status: String,
                       Version: String,
                       Device_Type: String,
                       ASC: String,
                       ASCQ: String,
                       Bytes_Per_Sector: String,
                       Total_Sectors: String,
                       Unformatted_Capacity: String
                     )

case class SCSIInformation(
                            Removable: String,
                            Failure_Prediction: List[String]
                          )

case class PartitionInformation(
                                 @JacksonXmlElementWrapper(useWrapping = false) Partition: List[Partition]
                               )

case class Partition(
                      Drive: String,
                      Total_Space: String,
                      Free_Space: String,
                      Free_Space_Percent: String,
                      Disk: String,
                      BlockSize: String,
                      Files: String,
                      FileSystem: String
                    )

