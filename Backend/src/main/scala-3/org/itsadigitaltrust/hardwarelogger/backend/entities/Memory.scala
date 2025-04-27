package org.itsadigitaltrust.hardwarelogger.backend.entities

import com.augustnagro.magnum.{DbCodec, Id, MySqlDbType, SqlName, SqlNameMapper, Table}

@Table(MySqlDbType)
final case class Memory(
                         @Id id: Long,
                         size: String,
                         @SqlName("itsaid") itsaID: String,
                         @SqlName("description") description: Option[String]
                       ) extends HLEntityWithItsaID derives DbCodec

final case class MemoryCreator(
                                size: String,
                                @SqlName("itsaid") itsaID: String,
                                description: Option[String] = None
                              ) extends HLEntityCreatorWithItsaID derives DbCodec
