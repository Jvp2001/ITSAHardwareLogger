package org.itsadigitaltrust.hardwarelogger.backend.entities

import com.augustnagro.magnum.*

@Table(MySqlDbType, SqlNameMapper.CamelToSnakeCase)
@SqlName("disks")
final case class Disk(
                       @Id id: Long,
                       itsaid: String,
                       model: String,
                       @SqlName("capicty") capacity: String,
                       serial: String,
                       `type`: String,
                       @SqlName("descr") description: String,
                     ) extends HLEntityWithItsaID derives DbCodec

final case class DiskCreator(
                              itsaid: String,
                              model: String,
                              @SqlName("capicty") capacity: String,
                              serial: String,
                              `type`: String,
                              @SqlName("descr") description: String,
                            ) extends HLEntityCreatorWithItsaID derives DbCodec