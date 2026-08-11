package org.itsadigitaltrust.hardwarelogger.backend

import com.augustnagro.magnum.{Repo, TableInfo}
import org.itsadigitaltrust.hardwarelogger.backend.entities.*
import org.itsadigitaltrust.hardwarelogger.backend.types.{EntityFromEC, ItsaEC}

import scala.quoted.*

/**
  * Compile-time macro helpers for summoning `DbCodec`, `Repo` and `TableInfo`
  * instances based on the concrete `EntityCreator` type `EC`.
  */
object macros