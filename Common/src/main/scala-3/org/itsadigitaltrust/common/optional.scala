package org.itsadigitaltrust.common

import scala.util.boundary

object optional:
  def apply[T](x: OptionalLabel ?=> T): Option[T] =
    boundary:
      Some(x)
    

  private type OptionalLabel = Label[None.type]
  type Label[T] = boundary.Label[T]

  extension [T](x: Option[T])
    def ?(using OptionalLabel): T =
      x.getOrElse(boundary.break(None))

export optional.?