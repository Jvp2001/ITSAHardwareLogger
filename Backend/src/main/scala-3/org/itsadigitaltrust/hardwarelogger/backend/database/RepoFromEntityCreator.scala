package org.itsadigitaltrust.hardwarelogger.backend.database

import org.itsadigitaltrust.hardwarelogger.backend.entities.{HLEntityCreatorWithSomeKindOfID, HLEntityWithSomeKindOfID}
import org.itsadigitaltrust.hardwarelogger.backend.repos
import org.itsadigitaltrust.hardwarelogger.backend.repos.{HLRepoWithID, MediaRepo}

import scala.compiletime.deferred



object RepoFromEntityCreator
  