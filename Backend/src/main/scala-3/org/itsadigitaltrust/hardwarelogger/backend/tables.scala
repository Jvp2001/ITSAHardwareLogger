package org.itsadigitaltrust.hardwarelogger.backend

import com.augustnagro.magnum.TableInfo
import entities.*
object tables:
  type HLTableInfo[EC <:HLEntityCreator, E <: HLEntity] = TableInfo[EC, E, Long]
  given DiskTable: HLTableInfo[DiskCreator, Disk] = TableInfo[DiskCreator, Disk, Long]
  given InfoTable: HLTableInfo[InfoCreator, Info] = TableInfo[InfoCreator, Info, Long]
  given MediaTable: HLTableInfo[MediaCreator, Media] = TableInfo[MediaCreator, Media, Long]
  given MemoryTable: HLTableInfo[MemoryCreator, Memory] = TableInfo[MemoryCreator, Memory, Long]
  
  


