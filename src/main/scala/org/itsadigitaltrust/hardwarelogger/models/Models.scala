package org.itsadigitaltrust.hardwarelogger.models


final case class GeneralInfo(
                              computerID: String,
                              description: String,
                              model: String,
                              vendor: String,
                              serial: String,
                              os: String
                            )


final case class HardDrive(
                            health: Int,
                            size: String,
                            model: String,
                            serial: String,
                            `type`: HardDriveType,
                            id: String = "NOT LOGGED",
                            isSSD: Boolean = false
                          )

enum HardDriveType:
  case SATA, NVME, PATA

final case class Media(var description: String, var handle: String)

final case class Memory(var size: Int, var description: String)

final case class Processor(
                            var chipType: String,
                            var speed: String,
                            var shortDescription: String,
                            var longDescription: String,
                            var serial: String,
                            var width: Int = 0,
                            var cores: Int = 0
                          )




type HardwareModel = GeneralInfo | HardDrive | Memory | Media | Processor

