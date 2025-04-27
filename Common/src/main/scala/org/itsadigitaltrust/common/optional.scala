package org.itsadigitaltrust.common

import scala.util.boundary

object optional:
  def apply[T](x: boundary.Label[None.type] ?=> T): Option[T] =
    boundary { Some(x) }

  type Label[T] = boundary.Label[T]

  extension[T](x: Option[T])
    def ?(using boundary.Label[None.type]): T =
      x.getOrElse(boundary.break(None))
export optional.?