package org.itsadigitaltrust.hardwarelogger.models


import org.itsadigitaltrust.common.Types.DataSize

import scala.annotation.experimental

sealed trait HLModel

//@ScalaSpringBeanComponent
final case class GeneralInfoModel(
                              computerID: String,
                              description: String,
                              model: String,
                              vendor: String,
                              serial: String,
                              os: String
                            ) extends HLModel


//@ScalaSpringBeanComponent
final case class HardDriveModel(
                            health: Int,
                            size: DataSize,
                            model: String,
                            serial: String,
                            `type`: HardDriveType,
                            id: String = "NOT LOGGED",
                            isSSD: Boolean = false
                          ) extends HLModel

//
//
//@Bean
enum HardDriveType extends Enum[HardDriveType]:
  case SATA, NVME, PATA

//@ScalaSpringBeanComponent
final case class MediaModel(description: String, handle: String) extends HLModel

//@ScalaSpringBeanComponent
final case class MemoryModel(size: DataSize, description: String) extends HLModel

//@ScalaSpringBeanComponent
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

