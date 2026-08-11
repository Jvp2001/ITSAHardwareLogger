package org.itsadigitaltrust.hardwarelogger.backend.entities

import com.augustnagro.magnum.*

@Table(MySqlDbType)
@SqlName("memory")
final case class Memory(
                         @Id id: Long,
                         size: String,
                         @SqlName("itsaid") itsaID: String,
                         @SqlName("description") description: Option[String]
                       ) extends HLEntityWithItsaID derives DbCodec

object Memory:
  val MemoryTable = TableInfo[MemoryCreator, Memory, Long]
  given memoryTable: TableInfo[MemoryCreator, Memory, Long] = TableInfo[MemoryCreator, Memory, Long]

final case class MemoryCreator(
                                size: String,
                                @SqlName("itsaid") itsaID: String,
                                @SqlName("description") description: Option[String] = None
                              ) extends HLEntityCreatorWithItsaID derives DbCodec
