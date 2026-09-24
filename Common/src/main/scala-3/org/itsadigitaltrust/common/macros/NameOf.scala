package org.itsadigitaltrust.common.macros

object NameOf:
  import scala.quoted.*
  transparent inline def apply[T]: String =
    ${nameOfImpl[T]}

  transparent inline def nameOf[T]: String = apply[T]

  private def nameOfImpl[T : Type](using q: Quotes): Expr[String] =
    import q.reflect.*
    Expr(Type.show[T])

end NameOf