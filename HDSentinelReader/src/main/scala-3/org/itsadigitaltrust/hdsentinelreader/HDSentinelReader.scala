package org.itsadigitaltrust.hdsentinelreader

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.`type`.TypeFactory
import com.fasterxml.jackson.dataformat.xml.{JacksonXmlModule, XmlMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.itsadigitaltrust.hdsentinelreader
import org.itsadigitaltrust.hdsentinelreader.data.HardDiskSummary

import java.io.File
import scala.io.Source
import scala.util.Using
import scala.xml.{Document, Elem}
import org.itsadigitaltrust.hdsentinelreader.xml.{XMLParser, \\>, given}


class HDSentinelReader:

  import scala.compiletime.constValue


  private val xmlParser = new XMLParser

  inline def read(xml: XMLFile | String | Elem): Unit =
    xmlParser.read(xml)

  inline def read[T](name: String, klazz: Class[T]): T =
    \(name)(using klazz)
  inline def \[T : Class](name: String): T =
    xmlParser \ name
  def getAllNodesInElementsStartingWith[T : Class](startingWith: String, childName: String): Seq[T] =
    given xmlMapper:XmlMapper = xmlParser.xmlMapper
    val nodes = xmlParser.getAllNodesStartingWith(startingWith)
    nodes.map: node =>
      node \\> childName







object HDSentinelReader:
  inline def apply(sudoPassword: String, inline outputFileName: XMLFile): HDSentinelReader =
    val reader = new HDSentinelReader
    if System.getProperty("os.name").toLowerCase.contains("linux") then {
      val xml = ProcessRunner(sudoPassword, outputFileName)
      println(s"XML: $xml")
      reader.read(xml)
    }
    else
      reader.read(outputFileName)
    reader

  inline def apply(elem: Elem): HDSentinelReader =
    val reader = new HDSentinelReader
    reader.xmlParser.read(elem)
    reader