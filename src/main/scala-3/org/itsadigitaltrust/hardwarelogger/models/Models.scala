package org.itsadigitaltrust.hardwarelogger.models


import org.itsadigitaltrust.common.Types.{DataSize, Percentage}
import org.itsadigitaltrust.common.types.DataSizeType.DataSize
import org.itsadigitaltrust.hardwarelogger.services.HLDatabaseService

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

enum HardDriveConnectionType extends Enum[HardDriveConnectionType]:
  case SATA, NVME, PATA, SCSI, SAS, UNKNOWN

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
