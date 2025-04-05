package org.itsadigitaltrust.hdsentinelreader.data


import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

final case class HardDiskSummary(
                            @JacksonXmlProperty(localName = "Hard_Disk_Number") hardDiskNumber: Int,
                            @JacksonXmlProperty(localName = "Interface") interfaceType: String,
                            @JacksonXmlProperty(localName = "Disk_Controller") diskController: String,
                            @JacksonXmlProperty(localName = "Disk_Location") diskLocation: String,
                            @JacksonXmlProperty(localName = "Hard_Disk_Model_ID") hardDiskModelId: String,
                            @JacksonXmlProperty(localName = "Firmware_Revision") firmwareRevision: String,
                            @JacksonXmlProperty(localName = "Hard_Disk_Serial_Number") hardDiskSerialNumber: String,
                            @JacksonXmlProperty(localName = "SSD_Controller") ssdController: String,
                            @JacksonXmlProperty(localName = "Total_Size") totalSize: String,
                            @JacksonXmlProperty(localName = "Power_State") powerState: String,
                            @JacksonXmlProperty(localName = "Logical_Drive_s") logicalDrives: String,
                            @JacksonXmlProperty(localName = "Current_Temperature") currentTemperature: String,
                            @JacksonXmlProperty(localName = "Maximum_Temperature_ever_measured") maximumTemperatureEverMeasured: String,
                            @JacksonXmlProperty(localName = "Minimum_Temperature_ever_measured") minimumTemperatureEverMeasured: String,
                            @JacksonXmlProperty(localName = "Daily_Average") dailyAverage: String,
                            @JacksonXmlProperty(localName = "Daily_Maximum") dailyMaximum: String,
                            @JacksonXmlProperty(localName = "Power_on_time") powerOnTime: String,
                            @JacksonXmlProperty(localName = "Estimated_remaining_lifetime") estimatedRemainingLifetime: String,
                            @JacksonXmlProperty(localName = "Health") health: String,
                            @JacksonXmlProperty(localName = "Performance") performance: String,
                            @JacksonXmlProperty(localName = "Description") description: String,
                            @JacksonXmlProperty(localName = "Tip") tip: String
                          )
object HardDiskSummary: 
  given Class[HardDiskSummary] = classOf[HardDiskSummary]
//import com.fasterxml.jackson.annotation.JsonCreator
//import com.fasterxml.jackson.dataformat.xml.annotation.{JacksonXmlElementWrapper, JacksonXmlProperty}
//
//import javax.xml.bind.annotation.XmlElement
//
////@NoArgConstructor
////@experimental
//case class HardDiskSentinel(
//                             @XmlElement(name = "General_Information")  @JacksonXmlElementWrapper generalInformation: GeneralInformation,
//                            @JacksonXmlElementWrapper @XmlElement(name = "Physical_Disk_Information_Disk_0") physicalDiskInformationDisk0: PhysicalDiskInformationDisk0,
//                            @JacksonXmlElementWrapper @XmlElement(name = "Partition_Information") partitionInformation: PartitionInformation,
//                            @JacksonXmlElementWrapper @XmlElement(name = "Windows_Volume_RAID_Information") windowsVolumeRaidInformation: WindowsVolumeRaidInformation,
//                             @JacksonXmlElementWrapper @XmlElement(name = "System_Management_Information") systemManagementInformation: SystemManagementInformation
//
//                           )
//
////@NoArgConstructor
////@experimental
//case class PartitionInformation(
//                                 @JacksonXmlElementWrapper(useWrapping = false)
//                                 @XmlElement(name = "Partition") partitions: List[Partition]
//                               )
//
////@NoArgConstructor
////@experimental
//case class Partition(
//                     @JacksonXmlElementWrapper @XmlElement(name = "Drive") drive: String,
//                     @JacksonXmlElementWrapper @XmlElement(name = "Total_Space") totalSpace: String,
//                     @JacksonXmlElementWrapper @XmlElement(name = "Free_Space") freeSpace: String,
//                     @JacksonXmlElementWrapper @XmlElement(name = "Free_Space_Percent") freeSpacePercent: String
//                    )
//
////@NoArgConstructor
////@experimental
//case class WindowsVolumeRaidInformation()
//
////@NoArgConstructor
////@experimental
//case class SystemManagementInformation(
//                                       @JacksonXmlElementWrapper @XmlElement(name = "Motherboard_Information") motherboardInformation: MotherboardInformation,
//                                       @JacksonXmlElementWrapper @XmlElement(name = "System_Information") systemInformation: SystemInformationDetails
//                                      )
//
////@NoArgConstructor
////@experimental
//case class MotherboardInformation(
//                                  @JacksonXmlElementWrapper @XmlElement(name = "Manufacturer") manufacturer: String,
//                                  @JacksonXmlElementWrapper @XmlElement(name = "Product") product: String
//                                 )
//
////@NoArgConstructor
////@experimental
//case class SystemInformationDetails(
//                                    @JacksonXmlElementWrapper @XmlElement(name = "Manufacturer") manufacturer: String,
//                                    @JacksonXmlElementWrapper @XmlElement(name = "Product") product: String,
//                                    @JacksonXmlElementWrapper @XmlElement(name = "Serial_number") serialNumber: String
//                                   )
////@NoArgConstructor
////@experimental
//case class GeneralInformation(
//                              @JacksonXmlElementWrapper @XmlElement(name = "Application_Information") applicationInformation: ApplicationInformation,
//                              @JacksonXmlElementWrapper @XmlElement(name = "Computer_Information") computerInformation: ComputerInformation,
//                              @JacksonXmlElementWrapper @XmlElement(name = "System_Information") systemInformation: SystemInformation,
//                              @JacksonXmlElementWrapper @XmlElement(name = "PCI_Device_Information") pciDeviceInformation: PciDeviceInformation
//                             )
//
////@NoArgConstructor
////@experimental
//case class ApplicationInformation(
//                                  @JacksonXmlElementWrapper @XmlElement(name = "Installed_version") installedVersion: String
//                                 )
//
////@NoArgConstructor
////@experimental
//case class ComputerInformation(
//                               @JacksonXmlElementWrapper @XmlElement(name = "Computer_Name") computerName: String,
//                               @JacksonXmlElementWrapper @XmlElement(name = "User_Name") userName: String
//                              )
//
////@NoArgConstructor
////@experimental
//case class SystemInformation(
//                             @JacksonXmlElementWrapper @XmlElement(name = "Windows_Version") windowsVersion: String
//                            )
//
////@NoArgConstructor
////@experimental
//case class PciDeviceInformation(
//                                @JacksonXmlElementWrapper @XmlElement(name = "PCI_bus_0_device_0_function_0") pciBus0Device0Function0: String,
//                                @JacksonXmlElementWrapper @XmlElement(name = "Errors") errors: Errors
//                               )
//
////@NoArgConstructor
////@experimental
//case class Errors(
//                  @JacksonXmlElementWrapper @XmlElement(name = "NumOfDrives") numOfDrives: Int,
//                   @JacksonXmlElementWrapper(useWrapping = false) @XmlElement(name = "DriveError") driveErrors: List[DriveError]
//                 )
//
////@NoArgConstructor
////@experimental
//case class DriveError(
//                       id: Int,
//                      @JacksonXmlElementWrapper @XmlElement(name = "Status") status: String,
//                      @JacksonXmlElementWrapper @XmlElement(name = "Time") time: String,
//                      @JacksonXmlElementWrapper @XmlElement(name = "ATAID") ataId: Int,
//                      @JacksonXmlElementWrapper @XmlElement(name = "ENABLE_SMART") enableSmart: Int,
//                      @JacksonXmlElementWrapper @XmlElement(name = "SMART_THRESHOLDS") smartThresholds: Int,
//                      @JacksonXmlElementWrapper @XmlElement(name = "SMART_VALUES") smartValues: Int
//                     )
//
////@NoArgConstructor
////@experimental
//case class PhysicalDiskInformationDisk0(
//                                        @JacksonXmlElementWrapper @XmlElement(name = "Hard_Disk_Summary") hardDiskSummary: HardDiskSummary,
//                                        @JacksonXmlElementWrapper @XmlElement(name = "ATA_Information") ataInformation: AtaInformation,
//                                        @JacksonXmlElementWrapper @XmlElement(name = "Acoustic_Management_Configuration") acousticManagementConfiguration: AcousticManagementConfiguration,
//                                        @JacksonXmlElementWrapper @XmlElement(name = "ATA_Features") ataFeatures: AtaFeatures,
//                                        @JacksonXmlElementWrapper @XmlElement(name = "SSD_Features") ssdFeatures: SsdFeatures,
//                                        @JacksonXmlElementWrapper @XmlElement(name = "S.M.A.R.T._Details") smartDetails: SmartDetails,
//                                        @JacksonXmlElementWrapper @XmlElement(name = "Security_Mode") securityMode: SecurityMode,
//                                        @JacksonXmlElementWrapper @XmlElement(name = "Serial_ATA_Features") serialAtaFeatures: SerialAtaFeatures,
//                                        @JacksonXmlElementWrapper @XmlElement(name = "Disk_Information") diskInformation: DiskInformation,
//                                         @JacksonXmlElementWrapper(useWrapping = false) @XmlElement(name = "S.M.A.R.T.") smart: List[SmartAttribute],
//                                        @JacksonXmlElementWrapper @XmlElement(name = "Transfer_Rate_Information") transferRateInformation: TransferRateInformation,
//                                         @JacksonXmlElementWrapper(useWrapping = false) @XmlElement(name = "Alert") alerts: List[Alert],
//                                         @JacksonXmlElementWrapper(useWrapping = false) @XmlElement(name = "LogEntry") log: List[LogEntry]
//                                       )
//
////@NoArgConstructor
////@experimental

////@NoArgConstructor
////@experimental
//case class AtaInformation(
//                          @JacksonXmlElementWrapper @XmlElement(name = "Hard_Disk_Cylinders") hardDiskCylinders: Long,
//                          @JacksonXmlElementWrapper @XmlElement(name = "Hard_Disk_Heads") hardDiskHeads: Int,
//                          @JacksonXmlElementWrapper @XmlElement(name = "Hard_Disk_Sectors") hardDiskSectors: Int,
//                          @JacksonXmlElementWrapper @XmlElement(name = "ATA_Revision") ataRevision: String,
//                          @JacksonXmlElementWrapper @XmlElement(name = "Transport_Version") transportVersion: String,
//                          @JacksonXmlElementWrapper @XmlElement(name = "Total_Sectors") totalSectors: Long,
//                          @JacksonXmlElementWrapper @XmlElement(name = "Bytes_Per_Sector") bytesPerSector: Int,
//                          @JacksonXmlElementWrapper @XmlElement(name = "Multiple_Sectors") multipleSectors: Int,
//                          @JacksonXmlElementWrapper @XmlElement(name = "Error_Correction_Bytes") errorCorrectionBytes: Int,
//                          @JacksonXmlElementWrapper @XmlElement(name = "Unformatted_Capacity") unformattedCapacity: String,
//                          @JacksonXmlElementWrapper @XmlElement(name = "Maximum_PIO_Mode") maximumPioMode: Int,
//                          @JacksonXmlElementWrapper @XmlElement(name = "Maximum_Multiword_DMA_Mode") maximumMultiwordDmaMode: Int,
//                          @JacksonXmlElementWrapper @XmlElement(name = "Highest_Possible_Transfer_Rate") highestPossibleTransferRate: String,
//                          @JacksonXmlElementWrapper @XmlElement(name = "Negotiated_Transfer_Rate") negotiatedTransferRate: String,
//                          @JacksonXmlElementWrapper @XmlElement(name = "Minimum_multiword_DMA_Transfer_Time") minimumMultiwordDmaTransferTime: String,
//                          @JacksonXmlElementWrapper @XmlElement(name = "Recommended_Multiword_DMA_Transfer_Time") recommendedMultiwordDmaTransferTime: String,
//                          @JacksonXmlElementWrapper @XmlElement(name = "Minimum_PIO_Transfer_Time_Without_IORDY") minimumPioTransferTimeWithoutIordy: String,
//                          @JacksonXmlElementWrapper @XmlElement(name = "Minimum_PIO_Transfer_Time_With_IORDY") minimumPioTransferTimeWithIordy: String,
//                          @JacksonXmlElementWrapper @XmlElement(name = "ATA_Control_Byte") ataControlByte: String,
//                          @JacksonXmlElementWrapper @XmlElement(name = "ATA_Checksum_Value") ataChecksumValue: String
//                         )
//
////@NoArgConstructor
////@experimental
//case class AcousticManagementConfiguration(
//                                           @JacksonXmlElementWrapper @XmlElement(name = "Acoustic_Management") acousticManagement: String,
//                                           @JacksonXmlElementWrapper @XmlElement(name = "Current_Acoustic_Level") currentAcousticLevel: String,
//                                           @JacksonXmlElementWrapper @XmlElement(name = "Recommended_Acoustic_Level") recommendedAcousticLevel: String
//                                          )
//
////@NoArgConstructor
////@experimental
//case class AtaFeatures(
//                       @JacksonXmlElementWrapper @XmlElement(name = "Read_Ahead_Buffer") readAheadBuffer: String,
//                       @JacksonXmlElementWrapper @XmlElement(name = "DMA") dma: String,
//                       @JacksonXmlElementWrapper @XmlElement(name = "Ultra_DMA") ultraDma: String,
//                       @JacksonXmlElementWrapper @XmlElement(name = "S.M.A.R.T.") smart: String,
//                       @JacksonXmlElementWrapper @XmlElement(name = "Power_Management") powerManagement: String,
//                       @JacksonXmlElementWrapper @XmlElement(name = "Write_Cache") writeCache: String,
//                       @JacksonXmlElementWrapper @XmlElement(name = "Host_Protected_Area") hostProtectedArea: String,
//                       @JacksonXmlElementWrapper @XmlElement(name = "Advanced_Power_Management") advancedPowerManagement: String,
//                       @JacksonXmlElementWrapper @XmlElement(name = "Extended_Power_Management") extendedPowerManagement: String,
//                       @JacksonXmlElementWrapper @XmlElement(name = "Power_Up_In_Standby") powerUpInStandby: String,
//                       @JacksonXmlElementWrapper @XmlElement(name = "XN_48_bit_LBA_Addressing") xn48BitLbaAddressing: String,
//                       @JacksonXmlElementWrapper @XmlElement(name = "Device_Configuration_Overlay") deviceConfigurationOverlay: String,
//                       @JacksonXmlElementWrapper @XmlElement(name = "IORDY_Support") iordySupport: String,
//                       @JacksonXmlElementWrapper @XmlElement(name = "Read_Write_DMA_Queue") readWriteDmaQueue: String,
//                       @JacksonXmlElementWrapper @XmlElement(name = "NOP_Command") nopCommand: String,
//                       @JacksonXmlElementWrapper @XmlElement(name = "Trusted_Computing") trustedComputing: String,
//                       @JacksonXmlElementWrapper @XmlElement(name = "XN_64_bit_World_Wide_ID") xn64BitWorldWideId: String,
//                       @JacksonXmlElementWrapper @XmlElement(name = "Streaming") streaming: String,
//                       @JacksonXmlElementWrapper @XmlElement(name = "Media_Card_Pass_Through") mediaCardPassThrough: String,
//                       @JacksonXmlElementWrapper @XmlElement(name = "General_Purpose_Logging") generalPurposeLogging: String,
//                       @JacksonXmlElementWrapper @XmlElement(name = "Error_Logging") errorLogging: String,
//                       @JacksonXmlElementWrapper @XmlElement(name = "CFA_Feature_Set") cfaFeatureSet: String,
//                       @JacksonXmlElementWrapper @XmlElement(name = "CFast_Device") cfastDevice: String,
//                       @JacksonXmlElementWrapper @XmlElement(name = "Long_Physical_Sectors_1") longPhysicalSectors1: String,
//                       @JacksonXmlElementWrapper @XmlElement(name = "Long_Logical_Sectors") longLogicalSectors: String,
//                       @JacksonXmlElementWrapper @XmlElement(name = "Write_Read_Verify") writeReadVerify: String,
//                       @JacksonXmlElementWrapper @XmlElement(name = "NV_Cache_Feature") nvCacheFeature: String,
//                       @JacksonXmlElementWrapper @XmlElement(name = "NV_Cache_Power_Mode") nvCachePowerMode: String,
//                       @JacksonXmlElementWrapper @XmlElement(name = "NV_Cache_Size") nvCacheSize: String,
//                       @JacksonXmlElementWrapper @XmlElement(name = "Free_fall_Control") freeFallControl: String,
//                       @JacksonXmlElementWrapper @XmlElement(name = "Free_fall_Control_Sensitivity") freeFallControlSensitivity: String,
//                       @JacksonXmlElementWrapper @XmlElement(name = "Nominal_Media_Rotation_Rate") nominalMediaRotationRate: String
//                      )
//
////@NoArgConstructor
////@experimental
//case class SsdFeatures(
//                       @JacksonXmlElementWrapper @XmlElement(name = "Data_Set_Management") dataSetManagement: String,
//                       @JacksonXmlElementWrapper @XmlElement(name = "TRIM_Command") trimCommand: String,
//                       @JacksonXmlElementWrapper @XmlElement(name = "Deterministic_Read_After_TRIM") deterministicReadAfterTrim: String,
//                       @JacksonXmlElementWrapper @XmlElement(name = "Operating_System_TRIM_Function") operatingSystemTrimFunction: String
//                      )
//
////@NoArgConstructor
////@experimental
//case class SmartDetails(
//                        @JacksonXmlElementWrapper @XmlElement(name = "Off_line_Data_Collection_Status") offlineDataCollectionStatus: String,
//                        @JacksonXmlElementWrapper @XmlElement(name = "Self_Test_Execution_Status") selfTestExecutionStatus: String,
//                        @JacksonXmlElementWrapper @XmlElement(name = "Total_Time_To_Complete_Off_line_Data_Collection") totalTimeToCompleteOfflineDataCollection: String,
//                        @JacksonXmlElementWrapper @XmlElement(name = "Execute_Off_line_Immediate") executeOfflineImmediate: String,
//                        @JacksonXmlElementWrapper @XmlElement(name = "Abort_restart_Off_line_By_Host") abortRestartOfflineByHost: String,
//                        @JacksonXmlElementWrapper @XmlElement(name = "Off_line_Read_Scanning") offlineReadScanning: String,
//                        @JacksonXmlElementWrapper @XmlElement(name = "Short_Self_test") shortSelfTest: String,
//                        @JacksonXmlElementWrapper @XmlElement(name = "Extended_Self_test") extendedSelfTest: String,
//                        @JacksonXmlElementWrapper @XmlElement(name = "Conveyance_Self_test") conveyanceSelfTest: String,
//                        @JacksonXmlElementWrapper @XmlElement(name = "Selective_Self_Test") selectiveSelfTest: String,
//                        @JacksonXmlElementWrapper @XmlElement(name = "Save_Data_Before_After_Power_Saving_Mode") saveDataBeforeAfterPowerSavingMode: String,
//                        @JacksonXmlElementWrapper @XmlElement(name = "Enable_Disable_Attribute_Autosave") enableDisableAttributeAutosave: String,
//                        @JacksonXmlElementWrapper @XmlElement(name = "Error_Logging_Capability") errorLoggingCapability: String,
//                        @JacksonXmlElementWrapper @XmlElement(name = "Short_Self_test_Estimated_Time") shortSelfTestEstimatedTime: String,
//                        @JacksonXmlElementWrapper @XmlElement(name = "Extended_Self_test_Estimated_Time") extendedSelfTestEstimatedTime: String,
//                        @JacksonXmlElementWrapper @XmlElement(name = "Last_Short_Self_test_Result") lastShortSelfTestResult: String,
//                        @JacksonXmlElementWrapper @XmlElement(name = "Last_Short_Self_test_Date") lastShortSelfTestDate: String,
//                        @JacksonXmlElementWrapper @XmlElement(name = "Last_Extended_Self_test_Result") lastExtendedSelfTestResult: String,
//                        @JacksonXmlElementWrapper @XmlElement(name = "Last_Extended_Self_test_Date") lastExtendedSelfTestDate: String
//                       )
//
////@NoArgConstructor
////@experimental
//case class SecurityMode(
//                        @JacksonXmlElementWrapper @XmlElement(name = "Security_Mode") securityMode: String,
//                        @JacksonXmlElementWrapper @XmlElement(name = "Security_Erase") securityErase: String,
//                        @JacksonXmlElementWrapper @XmlElement(name = "Security_Erase_Time") securityEraseTime: String,
//                        @JacksonXmlElementWrapper @XmlElement(name = "Security_Enhanced_Erase_Feature") securityEnhancedEraseFeature: String,
//                        @JacksonXmlElementWrapper @XmlElement(name = "Security_Enhanced_Erase_Time") securityEnhancedEraseTime: String,
//                        @JacksonXmlElementWrapper @XmlElement(name = "Security_Enabled") securityEnabled: String,
//                        @JacksonXmlElementWrapper @XmlElement(name = "Security_Locked") securityLocked: String,
//                        @JacksonXmlElementWrapper @XmlElement(name = "Security_Frozen") securityFrozen: String,
//                        @JacksonXmlElementWrapper @XmlElement(name = "Security_Counter_Expired") securityCounterExpired: String,
//                        @JacksonXmlElementWrapper @XmlElement(name = "Security_Level") securityLevel: String
//                       )
//
////@NoArgConstructor
////@experimental
//case class SerialAtaFeatures(
//                             @JacksonXmlElementWrapper @XmlElement(name = "S_ATA_Compliance") sataCompliance: String,
//                             @JacksonXmlElementWrapper @XmlElement(name = "S_ATA_I_Signaling_Speed_1.5_Gps") sataISignalingSpeed1_5Gps: String,
//                             @JacksonXmlElementWrapper @XmlElement(name = "S_ATA_II_Signaling_Speed_3_Gps") sataIISignalingSpeed3Gps: String,
//                             @JacksonXmlElementWrapper @XmlElement(name = "S_ATA_Gen3_Signaling_Speed_6_Gps") sataGen3SignalingSpeed6Gps: String,
//                             @JacksonXmlElementWrapper @XmlElement(name = "Receipt_Of_Power_Management_Requests_From_Host") receiptOfPowerManagementRequestsFromHost: String,
//                             @JacksonXmlElementWrapper @XmlElement(name = "PHY_Event_Counters") phyEventCounters: String,
//                             @JacksonXmlElementWrapper @XmlElement(name = "Non_Zero_Buffer_Offsets_In_DMA_Setup_FIS") nonZeroBufferOffsetsInDmaSetupFis: String,
//                             @JacksonXmlElementWrapper @XmlElement(name = "DMA_Setup_Auto_Activate_Optimization") dmaSetupAutoActivateOptimization: String,
//                             @JacksonXmlElementWrapper @XmlElement(name = "Device_Initiating_Interface_Power_Management") deviceInitiatingInterfacePowerManagement: String,
//                             @JacksonXmlElementWrapper @XmlElement(name = "In_Order_Data_Delivery") inOrderDataDelivery: String,
//                             @JacksonXmlElementWrapper @XmlElement(name = "Asynchronous_Notification") asynchronousNotification: String,
//                             @JacksonXmlElementWrapper @XmlElement(name = "Software_Settings_Preservation") softwareSettingsPreservation: String,
//                             @JacksonXmlElementWrapper @XmlElement(name = "Native_Command_Queuing_NCQ") nativeCommandQueuingNCQ: String,
//                             @JacksonXmlElementWrapper @XmlElement(name = "Queue_Length") queueLength: Int
//                            )
//
////@NoArgConstructor
////@experimental
//case class DiskInformation(
//                           @JacksonXmlElementWrapper @XmlElement(name = "Form_Factor") formFactor: String,
//                           @JacksonXmlElementWrapper @XmlElement(name = "Capacity") capacity: String,
//                           @JacksonXmlElementWrapper @XmlElement(name = "Disk_Interface") diskInterface: String,
//                           @JacksonXmlElementWrapper @XmlElement(name = "Device_Type") deviceType: String,
//                           @JacksonXmlElementWrapper @XmlElement(name = "Width") width: String,
//                           @JacksonXmlElementWrapper @XmlElement(name = "Depth") depth: String,
//                           @JacksonXmlElementWrapper @XmlElement(name = "Height") height: String
//                          )
//
////@NoArgConstructor
////@experimental
//case class SmartAttribute(
//                          @JacksonXmlElementWrapper @XmlElement(name = "ID") id: Int,
//                          @JacksonXmlElementWrapper @XmlElement(name = "Name") name: String,
//                          @JacksonXmlElementWrapper @XmlElement(name = "Threshold") threshold: Int,
//                          @JacksonXmlElementWrapper @XmlElement(name = "Value") value: Int,
//                          @JacksonXmlElementWrapper @XmlElement(name = "Worst") worst: Int,
//                          @JacksonXmlElementWrapper @XmlElement(name = "Data") data: String,
//                          @JacksonXmlElementWrapper @XmlElement(name = "Status") status: String,
//                          @JacksonXmlElementWrapper @XmlElement(name = "Flags") flags: String
//                         )
//
////@NoArgConstructor
////@experimental
//case class TransferRateInformation(
//                                   @JacksonXmlElementWrapper @XmlElement(name = "Total_Data_Read") totalDataRead: String,
//                                   @JacksonXmlElementWrapper @XmlElement(name = "Total_Data_Write") totalDataWrite: String,
//                                   @JacksonXmlElementWrapper @XmlElement(name = "Average_Reads_Per_Day") averageReadsPerDay: String,
//                                   @JacksonXmlElementWrapper @XmlElement(name = "Average_Writes_Per_Day") averageWritesPerDay: String,
//                                   @JacksonXmlElementWrapper @XmlElement(name = "Current_Transfer_Rate") currentTransferRate: String,
//                                   @JacksonXmlElementWrapper @XmlElement(name = "Maximum_Transfer_Rate") maximumTransferRate: String,
//                                   @JacksonXmlElementWrapper @XmlElement(name = "Current_Read_Rate") currentReadRate: String,
//                                   @JacksonXmlElementWrapper @XmlElement(name = "Current_Write_Rate") currentWriteRate: String
//                                  )
//
////@NoArgConstructor
////@experimental
//case class Alert(
//                 @JacksonXmlElementWrapper @XmlElement(name = "XN") xn: String
//                )
//
//
////@NoArgConstructor
////@experimental
//case class LogEntry(
//                     XN: String
//                   )
