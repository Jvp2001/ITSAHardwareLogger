package org.itsadigitaltrust.hardwarelogger.models


import scala.annotation.experimental


//@ScalaSpringBeanComponent
final case class GeneralInfoModel(
                              computerID: String,
                              description: String,
                              model: String,
                              vendor: String,
                              serial: String,
                              os: String
                            )


//@ScalaSpringBeanComponent
final case class HardDriveModel(
                            health: Int,
                            size: Long,
                            model: String,
                            serial: String,
                            `type`: HardDriveType,
                            id: String = "NOT LOGGED",
                            isSSD: Boolean = false
                          )

//
//
//@Bean
enum HardDriveType extends Enum[HardDriveType]:
  case SATA, NVME, PATA

//@ScalaSpringBeanComponent
final case class MediaModel(description: String, handle: String)

//@ScalaSpringBeanComponent
final case class MemoryModel(size: Long, description: String)

//@ScalaSpringBeanComponent
final case class ProcessorModel(
                            chipType: String,
                            speed: Long,
                            shortDescription: String,
                            longDescription: String,
                            serial: String,
                            width: Int = 0,
                            cores: Int = 0
                          )


type HardwareModel = GeneralInfoModel | HardDriveModel | MemoryModel | MediaModel | ProcessorModel

