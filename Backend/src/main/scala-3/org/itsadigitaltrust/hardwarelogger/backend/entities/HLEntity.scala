package org.itsadigitaltrust.hardwarelogger.backend.entities

import com.augustnagro.magnum.{DbCodec, Id}
trait HLEntity
trait HLEntityWithItsaID extends HLEntity:
  val itsaid: String
trait HLEntityCreator
trait HLEntityCreatorWithItsaID extends HLEntityCreator:
  val itsaid: String