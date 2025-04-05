package org.itsadigitaltrust.hdsentinelreader.xml

import com.fasterxml.jackson.dataformat.xml.{JacksonXmlModule, XmlMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import scala.xml.{Elem, XML}
import org.itsadigitaltrust.hdsentinelreader.Types.XMLFile

import scala.compiletime.{summonInline, uninitialized}
import scala.io.Source
import scala.util.Using
import java.io.{InputStream, StringReader}
import scala.reflect.ClassTag

private[hdsentinelreader] final class XMLParser:

  var xml: Elem = uninitialized

  private lazy val xmlMapper: XmlMapper =
    XmlMapper.builder()
      .addModules(DefaultScalaModule, new JacksonXmlModule())
      .build()


  def read(file: XMLFile | String | Elem): Unit =
    xml = file match
      case string: String => XML.load(new StringReader(string))
      case file if file.isInstanceOf[XMLFile] =>
        val xmlFile = file.asInstanceOf[XMLFile]
        if xmlFile.exists then
          XML.load(xmlFile.toURL)
        else
          throw new IllegalArgumentException(s"File ${xmlFile.toString} does not exist")
      case elem: Elem => elem
      case file: org.itsadigitaltrust.hdsentinelreader.Types.XMLFile =>
        XML.load(file.toURL)

 
  inline def \[T](name: String)(using Class[T]): T =
    val node = s"${xml \\ name}"
    xmlMapper.readValue[T](node, summonInline[Class[T]])

object XMLParser:
  def apply(file: XMLFile): XMLParser =
    val parser = new XMLParser()
    parser.read(file)
    parser


  
  
