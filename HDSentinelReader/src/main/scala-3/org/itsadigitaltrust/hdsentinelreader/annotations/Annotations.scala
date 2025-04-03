package org.itsadigitaltrust.hdsentinelreader.annotations
import scala.annotation.*

final case class XMLElementName(name: String) extends ConstantAnnotation
final case class XMLIgnore() extends StaticAnnotation
