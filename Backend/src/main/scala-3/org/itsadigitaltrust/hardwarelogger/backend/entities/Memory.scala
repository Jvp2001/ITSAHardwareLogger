package org.itsadigitaltrust.hardwarelogger.backend.entities

import com.augustnagro.magnum.{DbCodec, Id, MySqlDbType, SqlNameMapper, Table}

@Table(MySqlDbType)
final case class Memory(
                         @Id id: Long,
                         size: String,
                         itsaid: String,
                         descr: Option[String]
                       ) extends HLEntityWithItsaID derives DbCodec

final case class MemoryCreator(
                                size: String,
                                itsaid: String,
                                descr: Option[String] = None
                              ) extends HLEntityCreatorWithItsaID derives DbCodec
