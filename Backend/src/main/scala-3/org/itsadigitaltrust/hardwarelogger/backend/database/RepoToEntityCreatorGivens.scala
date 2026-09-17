package org.itsadigitaltrust.hardwarelogger.backend.database

import org.itsadigitaltrust.hardwarelogger.backend.entities.{Disk, DiskCreator, Info, InfoCreator, Media, MediaCreator, Memory, MemoryCreator, Wiping, WipingCreator}
import org.itsadigitaltrust.hardwarelogger.backend.repos
import org.itsadigitaltrust.hardwarelogger.backend.repos.HLRepoWithID

object RepoToEntityCreatorGivens:

  given infoRepoFromEC: RepoFromEntityCreator[InfoCreator, Info] with
    override given repo: HLRepoWithID[InfoCreator, Info] = repos.infoRepo

  given memoryRepoFromEC: RepoFromEntityCreator[MemoryCreator, Memory] with
    override given repo: HLRepoWithID[MemoryCreator, Memory] = repos.memoryRepo

  given diskRepoFromEC: RepoFromEntityCreator[DiskCreator, Disk] with
    override given repo: HLRepoWithID[DiskCreator, Disk] = repos.diskRepo


  given mediaRepoFromEC: RepoFromEntityCreator[MediaCreator, Media] with
    override given repo: HLRepoWithID[MediaCreator, Media] = repos.mediaRepo

  given wipingRepoFromEC: RepoFromEntityCreator[WipingCreator, Wiping] with
    override given repo: HLRepoWithID[WipingCreator, Wiping] = repos.wipingRepo


