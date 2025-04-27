package org.itsadigitaltrust.hardwarelogger.backend.entities

import com.augustnagro.magnum.Id


sealed trait HLEntity:
  @Id val id: Long
trait HLEntityWithItsaID extends HLEntity:
  val itsaID: String
trait HLEntityWithHardDiskID extends HLEntity:
  val hddID: String

sealed trait HLEntityCreator
trait HLEntityCreatorWithItsaID extends HLEntityCreator:
  val itsaID: String

trait HLEntityCreatorWithHardDiskID extends HLEntityCreator:
  val hddID: String