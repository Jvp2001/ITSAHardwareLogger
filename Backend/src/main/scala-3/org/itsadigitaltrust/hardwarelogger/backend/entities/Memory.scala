package org.itsadigitaltrust.hardwarelogger.backend.entities

import com.augustnagro.magnum.{DbCodec, Id, MySqlDbType, SqlNameMapper, Table}

@Table(MySqlDbType, SqlNameMapper.CamelToSnakeCase)
final case class Memory(
  id: Int,
  size: String,
  itsaId: String,
  descr: Option[String]
) derives DbCodec

final case class MemoryCreator(
  size: String,
  itsaId: String,
  descr: Option[String] = None
) derives DbCodec