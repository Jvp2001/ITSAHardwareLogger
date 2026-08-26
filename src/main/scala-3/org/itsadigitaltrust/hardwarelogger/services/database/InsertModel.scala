package org.itsadigitaltrust.hardwarelogger.services.database

import com.augustnagro.magnum.DbTx
import org.itsadigitaltrust.hardwarelogger.backend.entities.HLEntityCreator
import org.itsadigitaltrust.hardwarelogger.models.HLModel
import org.itsadigitaltrust.hardwarelogger.services.HardwareGrabberService
import org.itsadigitaltrust.hardwarelogger.services.database.mappers.ToEntityCreator


trait InsertModel[M <: HLModel, EC <: HLEntityCreator]:
  def add(model: M)(using
                    dbTx: DbTx,
                    hardwareGrabberService: HardwareGrabberService,
                    converter: ToEntityCreator[M, EC]
  ): Either[HLDatabaseServiceExceptionBase, Unit]

  def apply(model: M)(using
                    dbTx: DbTx,
                    hardwareGrabberService: HardwareGrabberService,
                      converter: ToEntityCreator[M, EC]
  ): Either[HLDatabaseServiceExceptionBase, Unit] = add(model)
  
end InsertModel

