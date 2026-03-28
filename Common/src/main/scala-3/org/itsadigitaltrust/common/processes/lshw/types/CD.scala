package org.itsadigitaltrust.common.processes.lshw.types

import com.fasterxml.jackson.annotation.{JsonFormat, JsonProperty, JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "id", visible = true)
@JsonSubTypes(Array(
  new JsonSubTypes.Type(value = classOf[CdRom], name = "cdrom"),
  new JsonSubTypes.Type(value = classOf[NvmeDisk], name = "namespace:0"),
  new JsonSubTypes.Type(value = classOf[NvmeDisk], name = "namespace:1"),
  new JsonSubTypes.Type(value = classOf[NvmeDisk], name = "namespace:2")
))
sealed trait Device

import com.fasterxml.jackson.annotation.JsonProperty

case class CdRom(
                  id: String,
                  @JsonProperty("class") `class`: String,
                  claimed: Boolean,
                  handle: String,
                  description: String,
                  product: String,
                  vendor: String,
                  @JsonProperty("physid") physId: String,
                  @JsonProperty("businfo") busInfo: String,
                  @JsonProperty("logicalname")
                  @JsonFormat(`with` = Array(JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY))
                  logicalName: Option[Seq[String]],
                  dev: String,
                  version: String,
                  configuration: CdRomConfiguration,
                  capabilities: CdRomCapabilities
                ) extends Device



case class CdRomConfiguration(
                               ansiversion: String,
                               status: String
                             )

case class CdRomCapabilities(
                              removable: String,
                              audio: String,
                              @JsonProperty("cd-r") cdR: String,
                              @JsonProperty("cd-rw") cdRw: String,
                              dvd: String,
                              @JsonProperty("dvd-r") dvdR: String,
                              @JsonProperty("dvd-ram") dvdRam: String
                            )

import com.fasterxml.jackson.annotation.JsonProperty

case class NvmeDisk(
                     id: String,
                     @JsonProperty("class") `class`: String,
                     claimed: Boolean,
                     handle: Option[String] = None,
                     description: String,
                     @JsonProperty("physid") physId: String,
                     @JsonProperty("businfo") busInfo: Option[String] = None,
                     @JsonProperty("logicalname")
                     @JsonFormat(`with` = Array(JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY))
                     logicalName: Option[Seq[String]],
                     units: Option[String] = None,
                     size: Option[Long] = None,
                     configuration: Option[NvmeConfiguration] = None,
                     capabilities: Option[NvmeCapabilities] = None
                   ) extends Device

case class NvmeConfiguration(
                              guid: Option[String] = None,
                              logicalsectorsize: Option[String] = None,
                              sectorsize: Option[String] = None,
                              wwid: Option[String] = None
                            )

case class NvmeCapabilities(
                             @JsonProperty("gpt-1.00") gpt: Option[String] = None,
                             partitioned: Option[String] = None,
                             @JsonProperty("partitioned:gpt") partitionedGpt: Option[String] = None
                           )

