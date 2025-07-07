package org.itsadigitaltrust.hardwarelogger.backend

import org.itsadigitaltrust.hardwarelogger.backend.entities.*

import com.augustnagro.magnum.DbCodec

import scala.reflect.ClassTag

object types:
  type ItsaEC = InfoCreator | MemoryCreator | DiskCreator | WipingCreator | MediaCreator | HLEntityCreator
  type ItsaEntity = Info | Memory | Disk | Media | entities.Wiping | HLEntity
  type HLTableInfoFromEC[EC <: ItsaEC] = EC match
    case MediaCreator => tables.mediaTable.type
    case InfoCreator => tables.infoTable.type
    case MemoryCreator => tables.memoryTable.type
    case DiskCreator => tables.diskTable.type
    case WipingCreator => tables.wipingTable.type
    case HLEntityCreatorWithItsaID | HLEntityCreatorWithHardDiskID => Nothing
  type EntityFromEC[EC <: ItsaEC] =
    EC match
      case MediaCreator => Media
      case InfoCreator => Info
      case MemoryCreator => Memory
      case DiskCreator => Disk
      case WipingCreator => Wiping
      case HLEntityCreator | HLEntityCreatorWithItsaID | HLEntityCreatorWithHardDiskID => Nothing
      case _ => Nothing
  type ECFromEntity[E <: ItsaEntity] =
    E match
      case Media => MediaCreator
      case Info => InfoCreator
      case Memory => MemoryCreator
      case Disk => DiskCreator
      case entities.Wiping => WipingCreator
      case HLEntity => Nothing
      case _ => Nothing
  type EntityClassTagFromEC[EC <: ItsaEC] = ClassTag[EntityFromEC[EC]]

  type DbCodecFromEC[EC <: ItsaEC] = DbCodec[EntityFromEC[EC]]


