package org.itsadigitaltrust.hardwarelogger.backend.entities

import com.augustnagro.magnum.{DbCodec, Id, MySqlDbType, SqlName, SqlNameMapper, Table}

@Table(MySqlDbType)
final case class Media(
                        @Id id: Long,
                        @SqlName("itsaid") itsaID: String,
                        @SqlName("description") description: String,
                        handle: Option[String]
                      ) extends HLEntityWithItsaID derives DbCodec

final case class MediaCreator(
                               @SqlName("itsaid") itsaID: String,
                               description: String,
                               handle: Option[String] = None
                             ) extends HLEntityCreatorWithItsaID derives DbCodec