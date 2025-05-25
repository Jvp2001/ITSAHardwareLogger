package org.itsadigitaltrust.hardwarelogger.backend

import com.augustnagro.magnum.Frag

object backend:
  export entities.entities.*
  export repos.given
  export repos.*
  export tables.given
  export tables.*
  export types.*
  export org.itsadigitaltrust.hardwarelogger.backend.{HLDatabase, DataSourceLoader}
  type Fragment = Frag
