package org.itsadigitaltrust.common

import scala.annotation.targetName

/**
 * A type to help the refactoring of the code that was in here into a sperate package.
 * This allows me to not have ot go through the rest of my code base and change the imports.
 */
object Types:
  export types.DataSize.*
  export types.Percentage.*

  export types.{DataSize, Percentage, dbString, toDouble, toString, value, unit}
  

export Types.*

