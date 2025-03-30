package org.itsadigitaltrust.hardwarelogger.backend.entities

import com.augustnagro.magnum.{DbCodec, Id, MySqlDbType, SqlName, SqlNameMapper, Table}

import java.sql.Timestamp
import java.time.{Instant, OffsetDateTime}

@Table(MySqlDbType, SqlNameMapper.SameCase)
@SqlName("info")
final case class Info(
                       @Id id: Long,
                       @SqlName("cpucores") cpuCores: Option[String],
                       @SqlName("cpudescription") cpuDescription: String,
                       @SqlName("cpuproduct") cpuProduct: Option[String],
                       @SqlName("cpuserial") cpuSerial: Option[String],
                       @SqlName("cpuspeed") cpuSpeed: String,
                       @SqlName("cpuvendor") cpuVendor: Option[String],
                       @SqlName("cpuwidth") cpuWidth: Option[String],
                       @SqlName("gendesc") genDesc: String,
                       @SqlName("genid") genId: String,
                       @SqlName("genproduct") genProduct: String,
                       @SqlName("genserial") genSerial: Option[String],
                       @SqlName("genvendor") genVendor: Option[String],
                       @SqlName("insertiondate") insertionDate: Timestamp,
                       itsaid: String,
                       @SqlName("lastupdated") lastUpdated: Timestamp,
                       @SqlName("OS") os: Option[String],
                       @SqlName("totalmemory") totalMemory: Option[String]
                     ) extends HLEntityWithItsaID derives DbCodec

final case class InfoCreator(
                              @SqlName("cpucores") cpuCores: Option[String],
                              @SqlName("cpudescription") cpuDescription: String,
                              @SqlName("cpuproduct") cpuProduct: Option[String],
                              @SqlName("cpuserial") cpuSerial: Option[String],
                              @SqlName("cpuspeed") cpuSpeed: String,
                              @SqlName("cpuvendor") cpuVendor: Option[String],
                              @SqlName("cpuwidth") cpuWidth: Option[String],
                              @SqlName("gendesc") genDesc: String,
                              @SqlName("genid") genId: String,
                              @SqlName("genproduct") genProduct: String,
                              @SqlName("genserial") genSerial: Option[String],
                              @SqlName("genvendor") genVendor: Option[String],
                              @SqlName("insertiondate") insertionDate: Timestamp,
                              itsaid: String,
                              @SqlName("lastupdated") lastUpdated: Timestamp,
                              @SqlName("OS") os: Option[String],
                              @SqlName("totalmemory") totalMemory: Option[String]
                            ) extends HLEntityCreatorWithItsaID derives DbCodec