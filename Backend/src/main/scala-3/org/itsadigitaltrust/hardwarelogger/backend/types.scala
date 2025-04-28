package org.itsadigitaltrust.hardwarelogger.backend

import org.itsadigitaltrust.hardwarelogger.backend.entities.WipingCreator
import org.itsadigitaltrust.hardwarelogger.backend.entities.entities.*

import scala.reflect.ClassTag

object types:
  type ItsaEC = InfoCreator | MemoryCreator | DiskCreator | WipingCreator | MediaCreator | HLEntityCreator
  type ItsaEntity = Info | Memory | Disk | Wiping | Media | HLEntity
  type EntityFromEC[EC <: ItsaEC] =
    EC match
      case MediaCreator => Media
      case InfoCreator => Info
      case MemoryCreator => Memory
      case DiskCreator => Disk
      case WipingCreator => Wiping
      case HLEntityCreator | HLEntityWithItsaID => HLEntity
  type ECFromEntity[E <: ItsaEntity] =
    E match
      case Media => MediaCreator
      case Info => InfoCreator
      case Memory => MemoryCreator
      case Disk => DiskCreator
      case Wiping => WipingCreator
      case HLEntity => HLEntityCreator
  type EntityClassTagFromEC[EC <: ItsaEC] = ClassTag[EntityFromEC[EC]]


