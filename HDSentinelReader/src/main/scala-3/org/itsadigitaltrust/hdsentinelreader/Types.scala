package org.itsadigitaltrust.hdsentinelreader

import java.io.File
import java.net.{URI, URL}

object Types:
  opaque type XMLFile = String

  extension (x: XMLFile)
    def toURI: URI = URI.create(this.toString)
    def toURL: URL = toURI.toURL
    def toFile: File = new File(toURI)

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