package org.itsadigitaltrust.common.processes.lshw.types

import com.fasterxml.jackson.annotation.{JsonProperty, JsonCreator}
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

// Base case class for the top-level array elements
sealed trait Device

@JsonDeserialize(as = classOf[CdRom])
case class CdRom(
                  id: String,
                  @JsonProperty("class") `class`: String, // JSON field is "class"
                  claimed: Boolean,
                  handle: String,
                  description: String,
                  product: String,
                  vendor: String,
                  physId: String, // Renamed from physid
                  busInfo: String, // Renamed from businfo
                  logicalName: List[String], // Renamed from logicalname
                  dev: String,
                  version: String,
                  configuration: CdRomConfiguration,
                  capabilities: CdRomCapabilities
                ) extends Device

case class CdRomConfiguration(
                               ansiVersion: String, // Renamed from ansiversion
                               status: String
                             )

case class CdRomCapabilities(
                              removable: String,
                              audio: String,
                              @JsonProperty("cd-r") cdR: String, // JSON field is "cd-r"
                              @JsonProperty("cd-rw") cdRw: String, // JSON field is "cd-rw"
                              dvd: String,
                              @JsonProperty("dvd-r") dvdR: String, // JSON field is "dvd-r"
                              @JsonProperty("dvd-ram") dvdRam: String // JSON field is "dvd-ram"
                            )

@JsonDeserialize(as = classOf[NvmeDisk])
case class NvmeDisk(
                     id: String,
                     @JsonProperty("class") `class`: String, // JSON field is "class"
                     claimed: Boolean,
                     description: String,
                     physId: String, // Renamed from physid
                     logicalName: Option[String] = None, // Renamed from logicalname
                     handle: Option[String] = None,
                     busInfo: Option[String] = None, // Renamed from businfo
                     units: Option[String] = None,
                     size: Option[Long] = None,
                     configuration: Option[NvmeDiskConfiguration] = None,
                     capabilities: Option[NvmeDiskCapabilities] = None
                   ) extends Device

case class NvmeDiskConfiguration(
                                  guid: Option[String] = None,
                                  logicalSectorSize: Option[String] = None, // Renamed from logicalsectorsize
                                  sectorSize: Option[String] = None, // Renamed from sectorsize
                                  wwid: Option[String] = None
                                )

case class NvmeDiskCapabilities(
                                 @JsonProperty("gpt-1.00") gpt100: Option[String] = None, // JSON field is "gpt-1.00"
                                 partitioned: Option[String] = None,
                                 @JsonProperty("partitioned:gpt") partitionedGpt: Option[String] = None // JSON field is "partitioned:gpt"
                               )