package org.itsadigitaltrust.hardwarelogger.backend

import com.augustnagro.magnum.Repo
import org.itsadigitaltrust.hardwarelogger.backend.entities.*

object repos:
  type HLRepo[EC, E] = Repo[EC, E, Long]
  given hddRepo: HLRepo[HddCreator, Hdd] = Repo[HddCreator, Hdd, Long]
  given mediaRepo: HLRepo[MediaCreator, Media] = Repo[MediaCreator, Media, Long]
  given infoRepo: HLRepo[InfoCreator, Info] = Repo[InfoCreator, Info, Long]
  given memoryRepo: HLRepo[MemoryCreator, Memory] = Repo[MemoryCreator, Memory, Long]


