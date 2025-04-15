package org.itsadigitaltrust.hardwarelogger.backend.entities

import com.augustnagro.magnum.{DbCodec, Id, MySqlDbType, SqlName, SqlNameMapper, Table}

import java.time.OffsetDateTime

@Table(MySqlDbType, SqlNameMapper.CamelToSnakeCase)
@SqlName("wiping")
final case class Wiping(
                         @Id int: Long,
                         @SqlName("hdd_id") hddId: String,
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
                       ) extends HLEntity derives DbCodec

final case class WipingCreator(
                                hddId: String,
                                serial: String,
                                model: String,
                                insertionDate: OffsetDateTime,
                                capacity: Option[String] = None,
                                `type`: Option[String] = None,
                                description: Option[String] = None,
                                health: Byte = -1,
                                toUpdate: Boolean = true,
                                isSsd: Boolean = false,
                                formFactor: Option[String] = None
                              ) extends HLEntity derives DbCodec


