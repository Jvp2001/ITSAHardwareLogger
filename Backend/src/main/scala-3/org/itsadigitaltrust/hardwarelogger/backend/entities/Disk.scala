package org.itsadigitaltrust.hardwarelogger.backend.entities

import com.augustnagro.magnum.*

@Table(MySqlDbType, SqlNameMapper.CamelToSnakeCase)
final case class Disk(
                       @Id id: Long,
                       itsaId: String,
                       model: String,
                       capacity: String,
                       `type`: String,
                       descr: String,
                     ) extends HLEntity derives DbCodec

final case class DiskCreator(
                              itsaId: String,
                              model: String,
                              capacity: String,
                              `type`: String,
                              descr: String,
                            ) extends HLEntityCreator derives DbCodec