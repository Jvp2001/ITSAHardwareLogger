package org.itsadigitaltrust.common

import org.itsadigitaltrust.common.Operators.||>

import scala.util.boundary
import scala.util.boundary.Label

object optional:

  import scala.util.boundary, boundary.break, boundary.Label

  object optional:
    inline def apply[T](inline body: Label[None.type] ?=> T): Option[T] =
       boundary(Some(body))


    extension [T](r: Option[T])
      inline def ?(using label: Label[None.type]): T = r match
        case Some(x) => x
        case None => break(None)

      inline def ??(default: T): Option[T] = r.orElse(Some(default))

      inline def ??(default: Option[Option[T]]): Option[T] = default.get

    extension[T](r: T | Null)
      inline def ?(using label: Label[None.type]): T =
        r match
          case x: T => x
          case _:Null => break(None)







