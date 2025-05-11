package org.itsadigitaltrust.hdsentinelreader

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.`type`.TypeFactory
import com.fasterxml.jackson.dataformat.xml.{JacksonXmlModule, XmlMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import java.io.File
import scala.io.Source
import scala.util.Using
import scala.xml.{Document, Elem}
import org.itsadigitaltrust.common.*
import data.HardDiskSummary
import org.itsadigitaltrust.common.processes.ProcessRunner
import xml.*

import scala.reflect.ClassTag


class HDSentinelReader:

  import scala.compiletime.constValue


  private val xmlParser = new XMLParser

  inline def read(xml: XMLFile | String | Elem): Unit =
    xmlParser.read(xml)

  inline def read[T : ClassTag](name: String): T =
    \(name)
  inline def \[T : ClassTag](name: String): T =
    xmlParser \ name
  def getAllNodesInElementsStartingWith[T : ClassTag](startingWith: String, childName: String): Seq[T] =
    given xmlMapper:XmlMapper = xmlParser.xmlMapper
    val nodes = xmlParser.getAllNodesStartingWith(startingWith)
    nodes.map: node =>
      node \\> childName







object HDSentinelReader:
  inline def apply[T: ClassTag](sudoPassword: String): HDSentinelReader =
    val reader = new HDSentinelReader
    if OSUtils.onLinux then
      val xml = ProcessRunner(sudoPassword)
      println(s"XML: $xml")
      reader.read(xml)
    reader

  inline def apply[T: ClassTag](elem: Elem): HDSentinelReader =
    val reader = new HDSentinelReader
    reader.read(elem)
    reader