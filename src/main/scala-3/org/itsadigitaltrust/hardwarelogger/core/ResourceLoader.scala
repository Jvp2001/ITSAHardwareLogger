package org.itsadigitaltrust.hardwarelogger.core

import java.net.URI

object ResourceLoader:
  private final val startPath = "org/itsadigitaltrust/hardwarelogger"

  extension(string: String)
    def toRootPath: String = s"$startPath/$string"

  def loadResource(string: String): URI =
    val value = s"$startPath/$string"
    ResourceLoader.getClass.getResource(value).toURI
    

end ResourceLoader

export ResourceLoader.toRootPath