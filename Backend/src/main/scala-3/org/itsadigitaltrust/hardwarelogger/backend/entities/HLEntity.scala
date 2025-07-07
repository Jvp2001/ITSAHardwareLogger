package org.itsadigitaltrust.hardwarelogger.backend.entities

import com.augustnagro.magnum.Id


private[backend] trait ItsaIDOfSomeKind:
  def getItsaID: String

private[backend] sealed trait HddID extends ItsaIDOfSomeKind: 
  val hddID: String

  override def getItsaID: String = hddID
  
private[backend] sealed trait ItsaID extends ItsaIDOfSomeKind:
  val itsaID: String

  override def getItsaID: String = itsaID
  

sealed trait HLEntity:
  @Id val id: Long
trait HLEntityWithItsaID extends HLEntity with ItsaID
trait HLEntityWithHardDiskID extends HLEntity with HddID


sealed trait HLEntityCreator
trait HLEntityCreatorWithItsaID extends HLEntityCreator with ItsaID
trait HLEntityCreatorWithHardDiskID extends HLEntityCreator with HddID
  

