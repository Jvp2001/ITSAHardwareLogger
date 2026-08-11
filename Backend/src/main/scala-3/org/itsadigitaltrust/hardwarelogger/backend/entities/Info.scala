package org.itsadigitaltrust.hardwarelogger.backend.entities

import com.augustnagro.magnum.*

import java.sql.Timestamp
import java.time.{Instant, OffsetDateTime}
import scala.Conversion.into
import scala.annotation.experimental
import scala.annotation.internal.preview
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
object Info:
  val InfoTable: TableInfo[InfoCreator, Info, Long] = TableInfo[InfoCreator, Info, Long]
  given infoTable: InfoTable.type = InfoTable

@experimental
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
                              @SqlName("insertiondate") insertionDate: into[Timestamp],
                              @SqlName("OS") os: Option[String],
                              @SqlName("cpucores") cpuCores: Option[String],
                              @SqlName("lastupdated") lastUpdated: into[Timestamp],
                            ) extends HLEntityCreatorWithItsaID derives DbCodec

