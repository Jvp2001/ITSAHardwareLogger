package org.itsadigitaltrust.hardwarelogger.backend.entities

import com.augustnagro.magnum.*

@Table(MySqlDbType)
@SqlName("disks")
final case class Disk(
                       @Id id: Long,
                       @SqlName("itsaid") itsaID: String,
                       model: String,
                       @SqlName("capicty") capacity: String,
                       serial: String,
                       `type`: String,
                       @SqlName("descr") description: String,
                     ) extends HLEntityWithItsaID derives DbCodec

final case class DiskCreator(
                              @SqlName("itsaid") itsaID: String,
                              model: String,
                              @SqlName("capicty") capacity: String,
                              serial: String,
                              `type`: String,
                              @SqlName("descr") description: String,
                            ) extends HLEntityCreatorWithItsaID derives DbCodec