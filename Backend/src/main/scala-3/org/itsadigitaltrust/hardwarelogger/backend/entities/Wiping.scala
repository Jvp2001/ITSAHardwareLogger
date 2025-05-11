package org.itsadigitaltrust.hardwarelogger.backend.entities

import com.augustnagro.magnum.{DbCodec, Id, MySqlDbType, SqlName, SqlNameMapper, Table}

import java.time.OffsetDateTime

@Table(MySqlDbType)
@SqlName("wiping")
final case class Wiping(
                         @SqlName("int") @Id id: Long,
                         @SqlName("hdd_id") hddID: String,
                         serial: String,
                         model: String,
                         @SqlName("insertiondate") insertionDate: OffsetDateTime,
                         @SqlName("capicty") capacity: Option[String],
                         `type`: Option[String],
                         @SqlName("descr") description: Option[String],
                         health: Byte,
                         @SqlName("to_update") toUpdate: Boolean,
                         @SqlName("is_ssd") isSsd: Boolean,
                         @SqlName("form_factor") formFactor: Option[String]
                       ) extends HLEntityWithHardDiskID derives DbCodec

final case class WipingCreator(
                                @SqlName("hdd_id") hddID: String,
                                serial: String,
                                model: String,
                                @SqlName("insertiondate") insertionDate: OffsetDateTime,
                                @SqlName("capicty") capacity: Option[String],
                                `type`: Option[String],
                                @SqlName("descr") description: Option[String],
                                health: Byte,
                                @SqlName("to_update") toUpdate: Boolean,
                                @SqlName("is_ssd") isSsd: Boolean,
                                @SqlName("form_factor") formFactor: Option[String]
                              ) extends HLEntityCreatorWithHardDiskID derives DbCodec


