package org.itsadigitaltrust.hardwarelogger.backend.entities

import com.augustnagro.magnum.{DbCodec, Id, MySqlDbType, SqlName, SqlNameMapper, Table}

import java.sql.Timestamp
import java.time.{Instant, OffsetDateTime}
import scala.languageFeature.implicitConversions
/**
 * DO NOT REORDER THE FIELDS IN EACH CLASS THAT IS MARKED WITH THE [[Table]] ANNOTATION. This will cause an [[java.sql.SQLDataException]] to be thrown.
 */
@Table(MySqlDbType)
@SqlName("info")
final case class Info(
                       @Id id: Long,
                       @SqlName("genid") genId: String,
                       @SqlName("gendesc") genDesc: String,
                       @SqlName("genproduct") genProduct: String,
                       @SqlName("genvendor") genVendor: Option[String],
                       @SqlName("itsaid") itsaID: String,
                       @SqlName("genserial") genSerial: Option[String],
                       @SqlName("totalmemory") totalMemory: Option[String],
                       @SqlName("cpudescription") cpuDescription: String,
                       @SqlName("cpuspeed") cpuSpeed: String,
                       @SqlName("cpuproduct") cpuProduct: Option[String],
                       @SqlName("cpuvendor") cpuVendor: Option[String],
                       @SqlName("cpuserial") cpuSerial: Option[String],
                       @SqlName("cpuwidth") cpuWidth: Option[String],
                       @SqlName("insertiondate") insertionDate: Timestamp,
                       @SqlName("OS") os: Option[String],
                       @SqlName("cpucores") cpuCores: Option[String],
                       @SqlName("lastupdated") lastUpdated: Timestamp,
                     ) extends HLEntityWithItsaID derives DbCodec

final case class InfoCreator(
                              @SqlName("genid") genId: String,
                              @SqlName("gendesc") genDesc: String,
                              @SqlName("genproduct") genProduct: String,
                              @SqlName("genvendor") genVendor: Option[String],
                              @SqlName("itsaid") itsaID: String,
                              @SqlName("genserial") genSerial: Option[String],
                              @SqlName("totalmemory") totalMemory: Option[String],
                              @SqlName("cpudescription") cpuDescription: String,
                              @SqlName("cpuspeed") cpuSpeed: String,
                              @SqlName("cpuproduct") cpuProduct: Option[String],
                              @SqlName("cpuvendor") cpuVendor: Option[String],
                              @SqlName("cpuserial") cpuSerial: Option[String],
                              @SqlName("cpuwidth") cpuWidth: Option[String],
                              @SqlName("insertiondate") insertionDate: Timestamp,
                              @SqlName("OS") os: Option[String],
                              @SqlName("cpucores") cpuCores: Option[String],
                              @SqlName("lastupdated") lastUpdated: Timestamp,
                            ) extends HLEntityCreatorWithItsaID derives DbCodec

