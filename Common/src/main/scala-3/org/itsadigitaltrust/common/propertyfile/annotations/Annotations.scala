package org.itsadigitaltrust.common.propertyfile.annotations

import scala.annotation.StaticAnnotation

sealed trait PropertyFileAnnotation(value: String) extends StaticAnnotation
final class Group(value: String) extends PropertyFileAnnotation(value)
final class Key(value: String) extends PropertyFileAnnotation(value)