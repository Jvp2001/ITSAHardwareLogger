package org.itsadigitaltrust.hardwarelogger.backend.entities

import com.augustnagro.magnum.Id


private[backend] transparent trait ItsaIDOfSomeKind:
  def getItsaID: String



private[backend] transparent sealed trait HddID extends ItsaIDOfSomeKind:
  val hddID: String

  override def getItsaID: String = hddID
  
private[backend] transparent sealed trait ItsaID extends ItsaIDOfSomeKind:
  val itsaID: String

  override def getItsaID: String = itsaID
  

sealed trait HLEntity:
  @Id val id: Long
trait HLEntityWithItsaID extends HLEntity with ItsaID
trait HLEntityWithHardDiskID extends HLEntity with HddID


sealed trait HLEntityCreator
transparent trait HLEntityCreatorWithItsaID extends HLEntityCreator with ItsaID
transparent trait HLEntityCreatorWithHardDiskID extends HLEntityCreator with HddID
  

