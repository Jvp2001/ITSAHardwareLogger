package org.itsadigitaltrust.common.configformats

import org.itsadigitaltrust.common.ConfigFilePath

import java.io.StringReader
import java.nio.file.{Files, Path}
import java.util.Properties
import scala.util.{Try, Using}

object JavaPropertiesReader:
  def read[T : PropertyReader](contents: String): Either[String, T] =
    val props = new Properties()
    
    Using(new StringReader(contents)): fis =>
      props.load(fis)
      val reader: PropertyReader[T] = summon[PropertyReader[T]]
       reader.read(props, "")
    .toEither.left.map(_.getMessage).flatten

  def readFile[T: PropertyReader](path: Path): Either[String, T] =
    Try(Files.readString(path))
      .toEither
      .left.map(error => s"Could not read properties file '$path': ${error.getMessage}")
      .flatMap(read[T])
