package org.itsadigitaltrust.hardwarelogger.models


import org.itsadigitaltrust.common.Types.{DataSize, Percentage}
import org.itsadigitaltrust.common.types.DataSizeType.DataSize
import org.itsadigitaltrust.hardwarelogger.services.HLDatabaseService
import org.itsadigitaltrust.hdsentinelreader.data.HDSentinelInterfaceTypeName

import scala.annotation.experimental

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


final case class HardDriveModel(
                                 health: Percentage,
                                 performance: Percentage,
                                 size: DataSize,
                                 model: String,
                                 serial: String,
                                 connectionType: HardDriveConnectionType,
                                 itsaID: String = "NOT LOGGED",
                                 `type`: "SSD" | "HHD" | "HDD" = "SSD",
                                 description: String = "",
                                 actions: String = "No actions needed.",
                                 powerOnTime: String = "",
                                 estimatedRemainingLifetime: String = ""
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

final case class MemoryModel(size: DataSize, description: String) extends HLModel

final case class ProcessorModel(
                                 name: String,
                                 speed: Long,
                                 shortDescription: String,
                                 longDescription: String,
                                 serial: String,
                                 width: Int = 0,
                                 cores: Int = 0
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
