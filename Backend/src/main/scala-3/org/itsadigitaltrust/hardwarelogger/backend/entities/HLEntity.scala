package org.itsadigitaltrust.hardwarelogger.backend.entities

import com.augustnagro.magnum.Id


 transparent trait ItsaIDOfSomeKind:
  def getItsaID: String

private[backend] transparent sealed trait HddID extends ItsaIDOfSomeKind:
  val hddID: String

  override def getItsaID: String = hddID
  
private[backend] transparent sealed trait ItsaID extends ItsaIDOfSomeKind:
  val itsaID: String

  override def getItsaID: String = itsaID
  
  
into sealed trait HLEntity extends Selectable:
  @Id val id: Long
trait HLEntityWithItsaID extends HLEntity with ItsaID
trait HLEntityWithHardDiskID extends HLEntity with HddID
type HLEntityWithSomeKindOfID = (HLEntityWithItsaID | HLEntityWithHardDiskID) & ItsaIDOfSomeKind

into sealed trait HLEntityCreator extends Selectable
into transparent trait HLEntityCreatorWithItsaID extends HLEntityCreator with ItsaID
into transparent trait HLEntityCreatorWithHardDiskID extends HLEntityCreator with HddID
type HLEntityCreatorWithSomeKindOfID = (HLEntityCreatorWithItsaID | HLEntityCreatorWithHardDiskID) & ItsaIDOfSomeKind