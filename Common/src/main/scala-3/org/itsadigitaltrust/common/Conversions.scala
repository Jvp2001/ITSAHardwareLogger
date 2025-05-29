package org.itsadigitaltrust.common

object Conversions:
  given intToBool: Conversion[Int, Boolean] =
    case 0 => false
    case _ => true
    
export Conversions.*
