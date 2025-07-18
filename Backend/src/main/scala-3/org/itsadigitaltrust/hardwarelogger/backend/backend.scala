package org.itsadigitaltrust.hardwarelogger.backend

import com.augustnagro.magnum.Frag

object backend:
  export entities.entities.*
  export repos.{findAllByID, findAllByIDStartingWith, insertOrUpdate, *}
  export repos.given
  export tables.given
  export tables.*
  export types.*
end backend

export backend.*