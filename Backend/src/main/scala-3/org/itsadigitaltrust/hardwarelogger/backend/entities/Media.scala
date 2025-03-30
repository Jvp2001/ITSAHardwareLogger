package org.itsadigitaltrust.hardwarelogger.backend.entities

import com.augustnagro.magnum.{DbCodec, Id, MySqlDbType, SqlName, SqlNameMapper, Table}

@Table(MySqlDbType, SqlNameMapper.CamelToSnakeCase)
final case class Media(
                        @Id id: Long,
                        @SqlName("itsaid") itsaid: String,
                        descr: String,
                        handle: Option[String]
                      ) extends HLEntityWithItsaID derives DbCodec

final case class MediaCreator(
                               itsaid: String,
                               descr: String,
                               handle: Option[String] = None
                             ) extends HLEntityCreatorWithItsaID derives DbCodec