package org.itsadigitaltrust.hardwarelogger.backend.entities

import com.augustnagro.magnum.{DbCodec, Id}

trait HLEntity:
  @Id val id: Long

trait ItsaIDEntity:
  val itsaID: String


trait DescEntity:
  val descr: String


trait ItsaEntity(

                ) extends HLEntity with ItsaIDEntity

trait ItsaDescriptionEntity extends ItsaEntity with DescEntity


