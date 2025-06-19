package org.itsadigitaltrust.common

import scala.annotation.targetName

/**
 * A type to help the refactoring of the code that was in here into a separate package.
 * This allows me to not have ot go through the rest of my code base and change the imports.
 */
object Types:
  export types.DataSizeType.*
  export types.PercentageType.*
  export types.Frequency.*
  export types.DataSizeType
  export Percentage.*

  

export Types.*

