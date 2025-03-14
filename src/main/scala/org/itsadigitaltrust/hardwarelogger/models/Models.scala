package org.itsadigitaltrust.hardwarelogger.models

import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.itsadigitaltrust.macros.ScalaSpringBeanComponent
import scala.annotation.experimental


@ScalaSpringBeanComponent
final case class GeneralInfo(
                              computerID: String,
                              description: String,
                              model: String,
                              vendor: String,
                              serial: String,
                              os: String
                            )


@ScalaSpringBeanComponent
final case class HardDrive(
                            health: Int,
                            size: String,
                            model: String,
                            serial: String,
                            `type`: HardDriveType,
                            id: String = "NOT LOGGED",
                            isSSD: Boolean = false
                          )

@Component
@Bean
enum HardDriveType extends Enum[HardDriveType]:
  case SATA, NVME, PATA

@ScalaSpringBeanComponent
final case class Media(description: String, handle: String)

@ScalaSpringBeanComponent
final case class Memory( size: Int,  description: String)

@ScalaSpringBeanComponent
final case class Processor(
                             chipType: String,
                             speed: String,
                             shortDescription: String,
                             longDescription: String,
                             serial: String,
                             width: Int = 0,
                             cores: Int = 0
                          )




type HardwareModel = GeneralInfo | HardDrive | Memory | Media | Processor

