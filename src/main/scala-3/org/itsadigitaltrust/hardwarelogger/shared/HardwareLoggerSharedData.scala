package org.itsadigitaltrust.hardwarelogger.shared

import org.itsadigitaltrust.hardwarelogger.models.HLModel.{GeneralInfoModel, HardDriveModel, MediaModel, MemoryModel, ProcessorModel}

final case class HardwareLoggerSharedData(
                                           itsaID: Option[String] = None,
                                           info: Option[Seq[GeneralInfoModel]] = None,
                                           memory: Option[Seq[MemoryModel]] = None,
                                           drives: Option[Seq[HardDriveModel]] = None,
                                           media: Option[Seq[MediaModel]] = None,
                                           processors: Option[Seq[ProcessorModel]] = None
                                         )
