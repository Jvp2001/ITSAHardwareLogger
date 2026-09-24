package org.itsadigitaltrust.hardwarelogger.ui.derivation.annotations

import scala.annotation.StaticAnnotation

sealed trait UISchemaAnnotation extends StaticAnnotation

final case class DisplayName(name: String) extends UISchemaAnnotation

final case class Readonly() extends UISchemaAnnotation

final case class Ignore() extends UISchemaAnnotation 

final case class Separator() extends UISchemaAnnotation 