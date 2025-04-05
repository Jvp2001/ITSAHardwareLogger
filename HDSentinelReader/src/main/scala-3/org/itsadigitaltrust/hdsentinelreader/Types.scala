package org.itsadigitaltrust.hdsentinelreader

import java.io.File
import java.net.{URI, URL}

object Types:
  opaque type XMLFile = String

  extension (x: XMLFile)
    inline def toURI: URI = URI.create(this.toString)
    inline def toURL: URL = toURI.toURL
    inline def toFile: File = new File(toURI)
    inline def exists: Boolean = toFile.exists

  object XMLFile:
    import scala.compiletime.*
    import scala.compiletime.ops.string.*
    inline def apply(file: String): XMLFile =
      inline if constValue[Matches[file.type, ".*\\.xml$"]] then
        file
      else
        scala.compiletime.error("Must end with .xml!")
    def from(file: String): XMLFile = file

    

export Types.*