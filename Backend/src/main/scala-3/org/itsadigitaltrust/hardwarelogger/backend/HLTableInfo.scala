package org.itsadigitaltrust.hardwarelogger.backend

import com.augustnagro.magnum.TableInfo


type HLTableInfo[EC <: entities.HLEntityCreator, E <: entities.HLEntity] = TableInfo[EC, E, Long]