package org.itsadigitaltrust.hardwarelogger.models


import org.itsadigitaltrust.common.Types.{DataSize, Percentage}
import org.itsadigitaltrust.common.types.DataSizeType.DataSize
import org.itsadigitaltrust.common.types.FrequencyType.Frequency
import org.itsadigitaltrust.hardwarelogger.services.HLDatabaseService
import org.itsadigitaltrust.hdsentinelreader.data.HDSentinelInterfaceTypeName

import scala.annotation.experimental

/**
 * @define tabs [[org.itsadigitaltrust.hardwarelogger.views.tabs]]
 */

/**
 * 
 */
sealed trait HLModel


final case class GeneralInfoModel(
                                   computerID: String,
                                   description: String, // The chassis-type
                                   model: String,
                                   vendor: String,
                                   serial: String,
                                   os: String,
                                   itsaID: Option[String] = None,
                                 ) extends HLModel:


end GeneralInfoModel

type HardDriveType = "SSD" | "HHD" | "HDD"

/**
 * The case class represents the information, for a single row, in the [[org.itsadigitaltrust.hardwarelogger.views.tabs.HardDriveTableView HardDriveTableView]] and [[org.itsadigitaltrust.hardwarelogger.dialogs.Dialogs.HardDriveExtraInformationDialog HardDriveExtraInformationDialog]].
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
                                 itsaID: String = "NOT LOGGED",
                                 `type`: HardDriveType = "SSD",
                                 description: String = "",
                                 actions: String = "No actions needed.",
                                 powerOnTime: String = "",
                                 estimatedRemainingLifetime: String = "",
                                 currentTemperature: String = "Unknown",
                                 maximumTemperature: String = "Unknown"
                               ) extends HLModel

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

private object exports:
  export HardDriveModel.*
  export HardDriveConnectionType.*
  export MediaModel.*
  export MemoryModel.*
  export ProcessorModel.*
  export GeneralInfoModel.*

export exports.*
