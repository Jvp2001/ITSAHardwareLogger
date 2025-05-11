package org.itsadigitaltrust.hdsentinelreader.xml

import com.fasterxml.jackson.dataformat.xml.{JacksonXmlModule, XmlMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import scala.xml.{Elem, Node, NodeSeq, XML, XMLReader}
import org.itsadigitaltrust.hdsentinelreader.Types.XMLFile

import scala.compiletime.{summonInline, uninitialized}
import scala.io.Source
import scala.util.Using
import java.io.{InputStream, StringReader}
import scala.reflect.{ClassTag, classTag}

private[hdsentinelreader] final class XMLParser:

  var xml: Elem = uninitialized

  private[hdsentinelreader] given xmlMapper: XmlMapper =
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


  def getAllNodesStartingWith(name: String): NodeSeq =
    xml.child.filter(_.label.startsWith(name))

  def \[T : ClassTag](name: String): T =
    xml \\> name

object XMLParser:
  def apply(file: XMLFile): XMLParser =
    val parser = new XMLParser()
    parser.read(file)
    parser
  given Conversion[XMLParser, XmlMapper]:
    override def apply(x: XMLParser): XmlMapper = x.xmlMapper

export XMLParser.given

extension(elem: Elem | Node)
  def \\>[T : ClassTag](name: String)(using xmlMapper: XmlMapper): T =
    val node = s"${elem \\ name}"
    println(node.length)
    val ct: ClassTag[T] = classTag[T]
    xmlMapper.readValue(node, ct.runtimeClass.asInstanceOf[Class[T]])

  
  
