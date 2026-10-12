package org.itsadigitaltrust.hardwarelogger.models


import org.itsadigitaltrust.common.Types.{DataSize, Percentage}
import org.itsadigitaltrust.common.processes.tlp.TlpBatteryStatus
import org.itsadigitaltrust.common.types.DataSizeType.DataSize
import org.itsadigitaltrust.common.types.FrequencyType.Frequency
import org.itsadigitaltrust.hardwarelogger.core.DataTypes.*
import org.itsadigitaltrust.hardwarelogger.services.HLDatabaseService
import org.itsadigitaltrust.hardwarelogger.ui.derivation.annotations.*
import org.itsadigitaltrust.hdsentinelreader.data.HDSentinelInterfaceTypeName

import scala.Conversion.into
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
  // Drives (Number of drives, status label (for not logged, bad health and no issues))

  enum GeneralDriveStatus:
    case BadHealth, NotLogged, NoIssues

    override def toString: String = this match
      case BadHealth => "One of the drives has a bad health Bad Health"
      case NotLogged => "Not Logged"
      case NoIssues => "No Issues"

  // bat health on general, cycles, age
  final case class GeneralInfoModel(
                                     @Readonly
                                     versionID: String = "",
                                     @Separator
                                     @Readonly
                                     description: String = "",

                                     /** The chassis-type */
                                     @Readonly
                                     model: String = "",
                                     @Readonly
                                     vendor: String = "",
                                     @Readonly
                                     serial: String = "",
                                     @Readonly
                                     @DisplayName("Chip Type")
                                     chipType: String = "",
                                     @Separator
                                     @Readonly
                                     amountOfMemory: String = "0GB",
                                     @Separator
                                     @Readonly
                                     amountOfStorage: String = "0GB",
                                     @Readonly
                                     @Ignore
                                     os: String = "",
                                     @Readonly
                                     numberOfDrives: Int = 0,
                                     @Readonly
                                     anyDriveIssues: Seq[GeneralDriveStatus] = Seq(GeneralDriveStatus.NoIssues),
                                     @Ignore
                                     itsaID: Option[String] = None,
                                     @Separator
                                     @Readonly
                                     batteryHealth: BatteryHealth = BatteryHealth(0.0),


                                   ) extends HLModelWithSerial:
    def getVendorID: String = if vendor == null then "ITSA Hardware Logger" else vendor

  end GeneralInfoModel

  type HardDriveType = "SSD" | "HHD" | "HDD" | "UNKNOWN"

  given Conversion[String, HardDriveType] with
    def apply(str: String): HardDriveType = (str.toUpperCase match
      case "SSD" => "SSD"
      case "HHD" => "HHD"
      case "HDD" => "HDD"
      case _ => "UNKNOWN"
      ).asInstanceOf[HardDriveType]
  end given


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
                                 ) extends HLModelWithSerial:
    def inGoodHealth: Boolean =
      `type` match
        case "SSD" if health.toDouble > 50.0 => true
        case "HDD" if health.toDouble == 100.0 => true
        case "HHD" if health.toDouble > 100 => true
        case _ => false

  object HardDriveModel:
    def hasLoggedItsaID(itsaID: String): Boolean =
      Option(itsaID).exists(id => id.trim.nonEmpty && !id.trim.equalsIgnoreCase("NOT LOGGED"))

  enum HardDriveConnectionType(name: HDSentinelInterfaceTypeName) extends Enum[HardDriveConnectionType]:
    case SATA extends HardDriveConnectionType("S-ATA II")
    case NVME extends HardDriveConnectionType("NVMe")
    case PATA extends HardDriveConnectionType("IDE/ATA")
    case SCSI extends HardDriveConnectionType("SCSI")
    case SAS extends HardDriveConnectionType("SAS")
    case UNKNOWN extends HardDriveConnectionType("UNKNOWN")
  end HardDriveConnectionType


  final case class MediaModel(description: String, handle: String) extends HLModel

  final case class MemoryModel(size: into[DataSize], description: String, `type`: String = "") extends HLModel

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

  import org.itsadigitaltrust.hardwarelogger.core.DataTypes.*

  final case class BatteryStatusModel(
                                       health: into[BatteryHealth],
                                       charge: Double,
                                       status: String,
                                       cycleCount: Option[Int],
                                       voltage: Option[Double],
                                     ) extends HLModel

  object BatteryStatusModel:

    def apply(status: TlpBatteryStatus): BatteryStatusModel =
      BatteryStatusModel(status.charge, status.capacity, status.status, status.cycleCount, status.voltage)

  type HardwareModel = GeneralInfoModel | HardDriveModel | MemoryModel | MediaModel | ProcessorModel | BatteryStatusModel

  type ItsaIDModel = GeneralInfoModel | HardDriveModel
end HLModel

private object exports:
  export HLModel.HardDriveModel
  export HLModel.HardDriveConnectionType
  export HLModel.MediaModel
  export HLModel.MemoryModel
  export HLModel.ProcessorModel
  export HLModel.GeneralInfoModel
  export HLModel.BatteryStatusModel
  export HLModel.GeneralDriveStatus
  export HLModel.HardwareModel
  export HLModel.ItsaIDModel

export exports.*
extension (memory: Seq[MemoryModel])
  def toTotalGBString: String =
    memory.map(_.size).toTotalGBString
  def toTotalGiBString: String =
    memory.map(_.size).toTotalGiBString