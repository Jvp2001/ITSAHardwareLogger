package org.itsadigitaltrust.hdsentinelreader

object Types:
  opaque type XMLFile = String

  object XMLFile:
    inline def apply(file: String): XMLFile =
      if !file.endsWith(".xml") then
        scala.compiletime.error("Must end with .xml!")
      file
export Types.*