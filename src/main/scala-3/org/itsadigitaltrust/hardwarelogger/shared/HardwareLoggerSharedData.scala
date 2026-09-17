package org.itsadigitaltrust.hardwarelogger.shared

import org.itsadigitaltrust.hardwarelogger.models.HLModel.*

final case class HardwareLoggerSharedData(
                                           oldItsaID: Option[String] = None,
                                           itsaID: Option[String] = None,
                                           info: Option[Seq[GeneralInfoModel]] = None,
                                           memory: Option[Seq[MemoryModel]] = None,
                                           drives: Option[Seq[HardDriveModel]] = None,
                                           media: Option[Seq[MediaModel]] = None,
                                           processors: Option[Seq[ProcessorModel]] = None,
                                           battery: Option[Seq[BatteryStatusModel]] = None,
                                         )
