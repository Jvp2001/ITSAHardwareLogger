package org.itsadigitaltrust.hardwarelogger.models


import org.itsadigitaltrust.common.Types.{DataSize, Percentage}
import org.itsadigitaltrust.common.types.DataSizeType.DataSize
import org.itsadigitaltrust.common.types.FrequencyType.Frequency
import org.itsadigitaltrust.hardwarelogger.services.HLDatabaseService
import org.itsadigitaltrust.hdsentinelreader.data.HDSentinelInterfaceTypeName

import scala.annotation.experimental

/**
 * tabs [[org.itsadigitaltrust.hardwarelogger.views.tabs]]
 */

/**
 *
 */
sealed trait HLModel extends Selectable
sealed trait HLModelWithSerial extends HLModel:
  def serial: String

object HLModel:
  
  final case class GeneralInfoModel(
                                     computerID: String,
                                     description: String, // The chassis-type
                                     model: String,
                                     vendor: String,
                                     serial: String,
                                     os: String,
                                     itsaID: Option[String] = None,
                                   ) extends HLModelWithSerial:
    def getVendorID: String = if vendor == null then "ITSA Hardware Logger" else vendor
  
  end GeneralInfoModel
  
  type HardDriveType = "SSD" | "HHD" | "HDD" | "UNKNOWN"
  
  /**
   * The case class represents the information, for a single row, in the [[org.itsadigitaltrust.hardwarelogger.views.tabs.HardDriveTableView HardDriveTableView]] and [[org.itsadigitaltrust.hardwarelogger.dialogs.Dialogs.HardDriveExtraInformationDialog HardDriveExtraInformationDialog]].
   *
   * @constructor Creates a new HardDriveModel with fields based on the parameters of this constructor  
   * @param health
   * @param performance
   * @param size
   * @param model
   * @param serial
   * @param connectionType
   * @param itsaID
   * @param `type`
   * @param description
   * @param actions
   * @param powerOnTime
   * @param estimatedRemainingLifetime
   * @param currentTemperature
   * @param maximumTemperature
   */
  final case class HardDriveModel(
                                   health: Percentage,
                                   performance: Percentage,
                                   size: DataSize,
                                   model: String,
                                   serial: String,
                                   connectionType: HardDriveConnectionType,
                                   var itsaID: String = "NOT LOGGED",
                                   `type`: HardDriveType = "SSD",
                                   description: String = "",
                                   actions: String = "No actions needed.",
                                   powerOnTime: String = "",
                                   estimatedRemainingLifetime: String = "",
                                   currentTemperature: String = "Unknown",
                                   maximumTemperature: String = "Unknown"
                                 ) extends HLModelWithSerial
  
  enum HardDriveConnectionType(name: HDSentinelInterfaceTypeName) extends Enum[HardDriveConnectionType]:
    case SATA extends HardDriveConnectionType("S-ATA II")
    case NVME extends HardDriveConnectionType("NVMe")
    case PATA extends HardDriveConnectionType("IDE/ATA")
    case SCSI extends HardDriveConnectionType("SCSI")
    case SAS extends HardDriveConnectionType("SAS")
    case UNKNOWN extends HardDriveConnectionType("UNKNOWN")
  end HardDriveConnectionType
  
  
  final case class MediaModel(description: String, handle: String) extends HLModel
  
  final case class MemoryModel(size: DataSize, description: String, `type`: String = "") extends HLModel
  
  final case class ProcessorModel(
                                   name: String,
                                   frequency: Frequency,
                                   shortDescription: String,
                                   longDescription: String,
                                   serial: String,
                                   width: Int = 0,
                                   cores: Int = 0,
                                   threads: Int = 0
                                 ) extends HLModel
  
  
  type HardwareModel = GeneralInfoModel | HardDriveModel | MemoryModel | MediaModel | ProcessorModel

  type ItsaIDModel = GeneralInfoModel | HardDriveModel
end HLModel

private object exports:
  export HLModel.HardDriveModel
  export HLModel.HardDriveConnectionType
  export HLModel.MediaModel
  export HLModel.MemoryModel
  export HLModel.ProcessorModel
  export HLModel.GeneralInfoModel
  export HLModel.HardwareModel
  export HLModel.ItsaIDModel

export exports.*
