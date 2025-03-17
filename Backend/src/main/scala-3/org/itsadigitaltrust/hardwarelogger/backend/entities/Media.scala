package org.itsadigitaltrust.hardwarelogger.backend.entities

import com.augustnagro.magnum.{DbCodec, Id, MySqlDbType, SqlNameMapper, Table}

@Table(MySqlDbType, SqlNameMapper.CamelToSnakeCase)
final case class Media(
  @Id id: Long,
  itsaId: String,
  descr: String,
  handle: Option[String]
) derives DbCodec

final case class MediaCreator(
  itsaId: String,
  descr: String,
  handle: Option[String] = None
) derives DbCodec