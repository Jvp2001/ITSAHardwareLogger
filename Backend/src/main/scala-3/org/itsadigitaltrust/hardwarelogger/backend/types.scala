package org.itsadigitaltrust.hardwarelogger.backend

import com.augustnagro.magnum.DbCodec
import org.itsadigitaltrust.hardwarelogger.backend.entities.*

import scala.reflect.ClassTag

object types:
  type ItsaEC = InfoCreator | MemoryCreator | entities.DiskCreator | WipingCreator | MediaCreator | HLEntityCreator
  type ItsaEntity = Info | Memory | Disk | entities.Media | entities.Wiping | HLEntity
  type HLTableInfoFromEC[EC <: ItsaEC] = EC match
    case MediaCreator => tables.mediaTable.type
    case InfoCreator => tables.infoTable.type
    case MemoryCreator => tables.memoryTable.type
    case entities.DiskCreator => tables.diskTable.type
    case WipingCreator => tables.wipingTable.type
    case HLEntityCreatorWithItsaID | HLEntityCreatorWithHardDiskID | HLEntityCreator => Nothing
    case _ => Nothing
  type EntityFromEC[EC <: ItsaEC] =
    EC match
      case MediaCreator => entities.Media
      case InfoCreator => Info
      case MemoryCreator => Memory
      case entities.DiskCreator => Disk
      case WipingCreator => Wiping
      case _ => Nothing
  type ECFromEntity[E <: ItsaEntity] =
    E match
      case entities.Media => MediaCreator
      case Info => InfoCreator
      case Memory => MemoryCreator
      case Disk => DiskCreator
      case entities.Wiping => WipingCreator
      case HLEntityCreatorWithItsaID | HLEntityCreatorWithHardDiskID | HLEntityCreator => Nothing
      case _ => Nothing
  type EntityClassTagFromEC[EC <: ItsaEC] = ClassTag[EntityFromEC[EC]]

  type DbCodecFromEC[EC <: ItsaEC] = DbCodec[EntityFromEC[EC]]

  type OptionalEntityFromEC[EC <: ItsaEC] = Option[EntityFromEC[EC]] 
