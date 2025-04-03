package org.itsadigitaltrust.hdsentinelreader.xml

import scala.xml.{Elem, XML}

import org.itsadigitaltrust.hdsentinelreader.Types.XMLFile

import scala.compiletime.uninitialized
import scala.io.Source
import scala.util.Using

private[hdsentinelreader] final class XMLParser:

  var xml: Elem = uninitialized

  def read(file: XMLFile): Unit =
    xml = XML.load(file.toURL)

object XMLParser:
  def apply(file: XMLFile): XMLParser =
    val parser = new XMLParser()
    parser.read(file)
    parser

  
  
