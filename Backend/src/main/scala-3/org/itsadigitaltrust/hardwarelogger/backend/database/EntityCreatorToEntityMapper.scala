package org.itsadigitaltrust.hardwarelogger.backend.database

import com.augustnagro.magnum.DbCodec
import org.itsadigitaltrust.common.GenericTypeMapper
import org.itsadigitaltrust.hardwarelogger.backend.entities.{Disk, DiskCreator, HLEntityCreatorWithSomeKindOfID, HLEntityWithSomeKindOfID, Info, InfoCreator, Media, MediaCreator, Memory, MemoryCreator, Wiping, WipingCreator}

trait EntityCreatorToEntityMapper[EC <: HLEntityCreatorWithSomeKindOfID : DbCodec] extends GenericTypeMapper[EC, HLEntityWithSomeKindOfID]:
  override type Out <: HLEntityWithSomeKindOfID : DbCodec


object EntityCreatorToEntityMapper:

  given infoECToInfo: EntityCreatorToEntityMapper[InfoCreator] with
    override type Out = Info

  given memoryECToMemory: EntityCreatorToEntityMapper[MemoryCreator] with
    override type Out = Memory

  given diskECToDisk: EntityCreatorToEntityMapper[DiskCreator] with
    override type Out = Disk

  given mediaECToMedia: EntityCreatorToEntityMapper[MediaCreator] with
    override type Out = Media

  given wipingECToWiping: EntityCreatorToEntityMapper[WipingCreator] with
    override type Out = Wiping

