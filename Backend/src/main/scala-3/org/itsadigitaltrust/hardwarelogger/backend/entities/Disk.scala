package org.itsadigitaltrust.hardwarelogger.backend.entities

import com.augustnagro.magnum.*

@Table(MySqlDbType)
@SqlName("disks")
final case class Disk(
                       @Id id: Long,
                       @SqlName("itsaid") itsaID: String,
                       model: String,
                       serial: String,
                       @SqlName("capicty") capacity: String,
                       `type`: String,
                       descr: String,
                     ) extends HLEntityWithItsaID derives DbCodec
object Disk:
  val DiskTable = TableInfo[DiskCreator, Disk, Long]
  given diskTable: TableInfo[DiskCreator, Disk, Long] = DiskTable

end Disk


final case class DiskCreator(
                              @SqlName("itsaid") itsaID: String,
                              model: String,
                              serial: String,
                              @SqlName("capicty") capacity: String,
                              `type`: String,
                              descr: String,
                            ) extends HLEntityCreatorWithItsaID derives DbCodec