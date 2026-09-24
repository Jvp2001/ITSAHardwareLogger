package org.itsadigitaltrust.common.configformats

import scala.annotation.*

sealed trait ConfigAnnotation extends StaticAnnotation
final case class ConfigGroupPrefix(prefix: String = "") extends ConfigAnnotation
final case class ConfigKey(key: String) extends ConfigAnnotation
final case class ConfigComment(text: String) extends ConfigAnnotation