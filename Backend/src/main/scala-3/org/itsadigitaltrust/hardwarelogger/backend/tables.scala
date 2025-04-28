package org.itsadigitaltrust.hardwarelogger.backend

import com.augustnagro.magnum.TableInfo
import entities.*
object tables:
  type HLTableInfo[EC <:HLEntityCreator, E <: HLEntity] = TableInfo[EC, E, Long]
  given diskTable: HLTableInfo[DiskCreator, Disk] = TableInfo[DiskCreator, Disk, Long]
  given infoTable: HLTableInfo[InfoCreator, Info] = TableInfo[InfoCreator, Info, Long]
  given mediaTable: HLTableInfo[MediaCreator, Media] = TableInfo[MediaCreator, Media, Long]
  given memoryTable: HLTableInfo[MemoryCreator, Memory] = TableInfo[MemoryCreator, Memory, Long]
  given wipingTable: HLTableInfo[WipingCreator, Wiping] = TableInfo[WipingCreator, Wiping, Long]



