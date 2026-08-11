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
                       @SqlName("description") description: String,
                     ) extends HLEntityWithItsaID derives DbCodec
object Disk:
  val DiskTable = TableInfo[DiskCreator, Disk, Long]
  given diskTable: TableInfo[DiskCreator, Disk, Long] = DiskTable
final case class DiskCreator(
                              @SqlName("itsaid") itsaID: String,
                              model: String,
                              @SqlName("capicty") capacity: String,
                              serial: String,
                              `type`: String,
                              @SqlName("description") description: String,
                            ) extends HLEntityCreatorWithItsaID derives DbCodec