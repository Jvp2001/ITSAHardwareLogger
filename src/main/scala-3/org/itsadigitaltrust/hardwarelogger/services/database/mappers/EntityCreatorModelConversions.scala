package org.itsadigitaltrust.hardwarelogger.services.database.mappers

import org.itsadigitaltrust.common.Operators.{?, ??, |>}
import org.itsadigitaltrust.common.types.DataSizeType.{DataSize, DataSizeUnit}
import org.itsadigitaltrust.common.types.FrequencyType.Frequency
import org.itsadigitaltrust.common.types.PercentageType.Percentage
import org.itsadigitaltrust.hardwarelogger.backend.entities.*
import org.itsadigitaltrust.hardwarelogger.core.{BuildInfo, total}
import org.itsadigitaltrust.hardwarelogger.models.HLModel.{GeneralInfoModel, HardDriveConnectionType, HardDriveModel, MediaModel, MemoryModel, ProcessorModel}
import org.itsadigitaltrust.hardwarelogger.services.database.{HLDatabaseServiceException, HLDatabaseServiceExceptionBase}
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{NotificationCentre, RequiresReadingItsaID}
import org.itsadigitaltrust.hardwarelogger.shared.HardwareLoggerSharedData
import ox.either.fail

import java.sql.Timestamp
import java.time.OffsetDateTime

private[database] given requiresReadingItsaID: RequiresReadingItsaID with
  override def itsaID: String = NotificationCentre.getFetchedData.flatMap(_.itsaID) ?? ""
  
  

private[database] object EntityCreatorModelConversions:
  private def appData: Option[HardwareLoggerSharedData] =
    NotificationCentre.getFetchedData

  given Conversion[OffsetDateTime, java.sql.Timestamp] with
    override def apply(x: OffsetDateTime): Timestamp =
      Timestamp.from(x.toInstant)


  given toInfo(using itsaID: RequiresReadingItsaID, genID: "itsa-hwlogger" = "itsa-hwlogger"): ToEntityCreator[GeneralInfoModel, InfoCreator] =
    infoModel =>
      ox.either:
        val data: HardwareLoggerSharedData = NotificationCentre.getFetchedData ?? HLDatabaseServiceException.unknownError("Unknown error has occurred when fetching the data").fail()
        val processor = data.processors.map(_.head) ?? HLDatabaseServiceException.unknownError("No processors found").fail()


        val totalMemory = NotificationCentre.getFetchedData.map(_.memory.total)
          ?? HLDatabaseServiceException.unknownError("Unknown error has occurred when fetching the total memory").fail()

        InfoCreator(cpuVendor = infoModel.vendor.?,
          genId = genID + s" ${BuildInfo.version}",
          itsaID = itsaID.itsaID,
          cpuSerial = processor.serial.?,
          insertionDate = OffsetDateTime.now(),
          lastUpdated = OffsetDateTime.now(),
          os = infoModel.os.?,
          cpuProduct = infoModel.model.?,
          cpuSpeed = processor.frequency.dbString,
          cpuCores = processor.cores.toString.?,
          cpuWidth = processor.width.toString.?,
          cpuDescription = processor.longDescription,
          genDesc = infoModel.description,
          genProduct = infoModel.computerID,
          genVendor = infoModel.vendor.?,
          genSerial = infoModel.serial.?,
          totalMemory = totalMemory.toString.?
        )

  end toInfo

  private[mappers] given createInfoModel: FromEntity[Info, GeneralInfoModel] =
    info =>
      GeneralInfoModel(
        computerID = info.genId,
        itsaID = info.itsaID.?,
        vendor = info.cpuVendor ?? "",
        serial = info.cpuSerial ?? "",
        os = info.os ?? "",
        model = info.cpuProduct ?? "",
        description = info.genDesc,
      )

  end createInfoModel


  extension [EC <: HLEntityCreator](ec: EC)
    private inline def toEither: Either[HLDatabaseServiceExceptionBase, EC] =
      Option(ec).toRight(HLDatabaseServiceException.unknownError("Unknown error has occurred when creating the entity"))

  private[mappers] given createProcessorModel: FromEntity[Info, ProcessorModel] =
    info =>
      ProcessorModel(
        name = info.cpuProduct ?? "",
        frequency = Frequency(info.cpuSpeed),
        shortDescription = "",
        longDescription = info.cpuDescription,
        serial = info.cpuSerial ?? "",
        width = info.cpuWidth.map(_.toInt) ?? 64,
        cores = info.cpuCores.map(_.toInt) ?? -1,
        threads = info.cpuCores.map(_.toInt) ?? -1,
      )
  end createProcessorModel

  given toProcessor(using genID: "itsa-hwlogger" = "itsa-hwlogger"): ToEntityCreator[ProcessorModel, InfoCreator] =
    processorModel =>
      ox.either:

        val data: HardwareLoggerSharedData = NotificationCentre.getFetchedData ?? HLDatabaseServiceException.unknownError("Unknown error has occurred when fetching the data").fail()
        val infoModel = data.info.head.headOption ?? HLDatabaseServiceException.unknownError("No info found").fail()
        val processor = data.processors.map(_.head) ?? HLDatabaseServiceException.unknownError("No processors found").fail()


        val totalMemory = NotificationCentre.getFetchedData.map(_.memory.total)
          ?? HLDatabaseServiceException.unknownError("Unknown error has occurred when fetching the total memory").fail()

        InfoCreator(cpuVendor = infoModel.vendor.?,
          genId = genID + s" ${BuildInfo.version}",
          itsaID = requiresReadingItsaID.itsaID,
          cpuSerial = processor.serial.?,
          insertionDate = OffsetDateTime.now(),
          lastUpdated = OffsetDateTime.now(),
          os = infoModel.os.?,
          cpuProduct = infoModel.model.?,
          cpuSpeed = processor.frequency.dbString,
          cpuCores = processor.cores.toString.?,
          cpuWidth = processor.width.toString.?,
          cpuDescription = processor.longDescription,
          genDesc = infoModel.description,
          genProduct = infoModel.computerID,
          genVendor = infoModel.vendor.?,
          genSerial = infoModel.serial.?,
          totalMemory = totalMemory.toString.?
        )


  given toMemory: ToEntityCreator[MemoryModel, MemoryCreator] =
    memoryModel =>
      MemoryCreator(memoryModel.size.dbString, requiresReadingItsaID.itsaID, memoryModel.description.?).toEither

  private[mappers] given createMemoryModel: FromEntity[Memory, MemoryModel] =
    memory =>
      MemoryModel(
        size = DataSize.from(memory.size) ?? DataSize(0, DataSizeUnit.GB),
        description = memory.descr ?? ""
      )

  given toMedia: ToEntityCreator[MediaModel, MediaCreator] =
    mediaModel =>
      MediaCreator(requiresReadingItsaID.itsaID, mediaModel.description.?, mediaModel.handle.?).toEither
  end toMedia

  given toHardDrive(using itsaID: RequiresReadingItsaID): ToEntityCreator[HardDriveModel, DiskCreator] =
    hardDriveModel =>
      DiskCreator(itsaID.itsaID,
        model = hardDriveModel.model,
        capacity = hardDriveModel.size.dbString,
        serial = hardDriveModel.serial,
        `type` = hardDriveModel.connectionType.toString,
        descr = "ATA Disk").toEither

  private[mappers] given createHardDriveModel: FromEntity[Disk, HardDriveModel] =
    hardDrive =>
      HardDriveModel(
        model = hardDrive.model,
        size = DataSize(0, DataSizeUnit.GB),
        serial = hardDrive.serial,
        description = hardDrive.descr ?? "",
        health = Percentage(100),
        performance = Percentage(100),
        connectionType = HardDriveConnectionType.NVME
      )

  end createHardDriveModel

  given toWiping: ToEntityCreator[HardDriveModel, WipingCreator] =
    model =>
      new WipingCreator(
        hddID = model.itsaID,
        serial = model.serial,
        model = model.model,
        insertionDate = OffsetDateTime.now(),
        capacity = model.size.dbString |> Option[String],
        `type` = model.`type`.?,
        description = model.description.?,
        health = model.health.toByte,
        toUpdate = true,
        isSsd = true,
        formFactor = "".?
      ).toEither

  private[mappers] given createHardDriveWipingModel: FromEntity[Wiping, HardDriveModel] =
    hardDrive =>
      HardDriveModel(
        model = hardDrive.model,
        size = DataSize(0, DataSizeUnit.GB),
        serial = hardDrive.serial,
        description = hardDrive.description ?? "",
        health = Percentage(100),
        performance = Percentage(100),
        connectionType = HardDriveConnectionType.NVME
      )


  given createMediaModel: FromEntity[Media, MediaModel] with
    override def apply(media: Media): MediaModel =
      MediaModel(
        description = media.description ?? "",
        handle = media.handle ?? ""
      )
  end createMediaModel
end EntityCreatorModelConversions
