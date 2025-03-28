package org.itsadigitaltrust.hardwarelogger.backend.entities

import com.augustnagro.magnum.{DbCodec, Id, MySqlDbType, SqlNameMapper, Table}

import java.time.OffsetDateTime

@Table(MySqlDbType, SqlNameMapper.CamelToSnakeCase)
final case class Info(
                       id: Int,
                       genId: String,
                       genDesc: String,
                       genProduct: String,
                       genVendor: Option[String],
                       itsaid: String,
                       genSerial: Option[String],
                       totalMemory: Option[String],
                       cpuDescription: String,
                       cpuSpeed: String,
                       cpuProduct: Option[String],
                       cpuVendor: Option[String],
                       cpuSerial: Option[String],
                       cpuWidth: Option[String],
                       insertionDate: OffsetDateTime,
                       os: String,
                       cpuCores: Option[String],
                       lastUpdated: OffsetDateTime
                     ) extends HLEntity derives DbCodec

final case class InfoCreator(
                              genId: String,
                              genDesc: String,
                              genProduct: String,
                              genVendor: Option[String],
                              itsaid: String,
                              genSerial: Option[String],
                              totalMemory: Option[String],
                              cpuDescription: String,
                              cpuSpeed: String,
                              cpuProduct: Option[String],
                              cpuVendor: Option[String],
                              cpuSerial: Option[String],
                              cpuWidth: Option[String],
                              os: String,
                              cpuCores: Option[String]
                            ) extends HLEntityCreator derives DbCodec