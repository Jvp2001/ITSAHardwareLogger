package org.itsadigitaltrust.common

import org.itsadigitaltrust.common.Operators.toOption

object Conversions:
  given intToBool: Conversion[Int, Boolean] =
    case 0 => false
    case _ => true
    
  given[A]: Conversion[A, Option[A]] = _.toOption
    
export Conversions.*
