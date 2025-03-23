package org.itsadigitaltrust.hardwarelogger.backend

import com.augustnagro.magnum.*
import entities.*
import entities.given

object repos:
  class HLRepo[EC, E](using defualt: RepoDefaults[EC, E, Long]) extends Repo[EC, E, Long]:
    def insertOrUpdate(creator: EC)(using DbTx)(using table: TableInfo[EC, E, Long]): Unit =
      sql"""insert into $table ${table.insertColumns} values ($creator) on duplicate key update ($creator)""".update.run()
  given DiskRepo: HLRepo[DiskCreator, Disk] = HLRepo[DiskCreator, Disk]

  given wipingRepo: HLRepo[WipingCreator, Wiping] = HLRepo[WipingCreator, Wiping]

  given MediaRepo: HLRepo[MediaCreator, Media] = HLRepo[MediaCreator, Media]

  given InfoRepo: HLRepo[InfoCreator, Info] = HLRepo[InfoCreator, Info]

  given MemoryRepo: HLRepo[MemoryCreator, Memory] = HLRepo[MemoryCreator, Memory]


