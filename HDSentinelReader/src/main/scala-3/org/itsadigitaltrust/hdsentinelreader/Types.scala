package org.itsadigitaltrust.hdsentinelreader

object Types:
  opaque type XMLFile = String

  object XMLFile:
    import scala.compiletime.*
    import scala.compiletime.ops.string.*
    inline def apply(file: String): XMLFile =
      inline if constValue[Matches[file.type, ".*\\.xml$"]] then
        file
      else
        scala.compiletime.error("Must end with .xml!")


export Types.*