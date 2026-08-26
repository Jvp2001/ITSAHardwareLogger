package org.itsadigitaltrust.common

import ox.Ox

import scala.annotation.targetName
import scala.util.boundary.Label

/**
 * A type to help the refactoring of the code that was in here into a separate package.
 * This allows me to not have to go through the rest of my code base and change the imports.
 */
object Types:
  export types.DataSizeType.*
  export types.PercentageType.*
  export types.Frequency.*
  export types.DataSizeType
  export Percentage.*

  type PrimitiveType = String | Float | Long | Double | Short | Boolean
trait GenericTypeMapper[In, OutConstraints]:
  type Out <: OutConstraints
export Types.*

