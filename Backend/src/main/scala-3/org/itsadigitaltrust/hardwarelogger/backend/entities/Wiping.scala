package org.itsadigitaltrust.hardwarelogger.backend.entities

import com.augustnagro.magnum.{DbCodec, Id, MySqlDbType, SqlNameMapper, Table}

import java.time.OffsetDateTime

@Table(MySqlDbType, SqlNameMapper.CamelToSnakeCase)
final case class Wiping(
                         @Id int: Long,
                         hddId: String,
                         serial: String,
                         model: String,
                         insertionDate: OffsetDateTime,
                         capacity: Option[String],
                         `type`: Option[String],
                         descr: Option[String],
                         health: Byte,
                         toUpdate: Boolean,
                         isSsd: Boolean,
                         formFactor: Option[String]
                       ) extends HLEntity derives DbCodec

final case class WipingCreator(
                                hddId: String,
                                serial: String,
                                model: String,
                                capacity: Option[String] = None,
                                `type`: Option[String] = None,
                                descr: Option[String] = None,
                                health: Byte = -1,
                                toUpdate: Boolean = true,
                                isSsd: Boolean = false,
                                formFactor: Option[String] = None
                              ) extends HLEntity derives DbCodec


