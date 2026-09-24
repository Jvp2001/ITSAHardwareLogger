package org.itsadigitaltrust.common.configformats

import org.itsadigitaltrust.common.ConfigFilePath

import java.io.{FileInputStream, StringReader}
import java.util.Properties
import scala.util.{Failure, Using}

object JavaPropertiesReader:
  def read[T : PropertyReader](contents: String): Either[String, T] =
    val props = new Properties()
    
    Using(new StringReader(contents)): fis =>
      props.load(fis)
      println(props)
      val reader: PropertyReader[T] = summon[PropertyReader[T]]
       reader.read(props, "")
    .toEither.left.map(_.getMessage).flatten


