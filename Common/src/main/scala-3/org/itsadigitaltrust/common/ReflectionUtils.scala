package org.itsadigitaltrust.common


import scala.compiletime.{constValue, constValueTuple, erasedValue, summonFrom, summonInline}

object ReflectionUtils:
  import scala.deriving.Mirror
  import scala.reflect.ClassTag

  // Recursively extracts literal string representations of case class field names, or names of enum cases
  transparent inline def summonLabels[T](using m: Mirror.ProductOf[T]): List[String] =
    constValueTuple[m.MirroredElemLabels].toList.asInstanceOf[List[String]]


  inline def toMap[T <: Product](obj: T)(using m: Mirror.ProductOf[T]): Map[String, Any] =
    // 1. Get field names as a list of strings
    val labels = constValueTuple[m.MirroredElemLabels].productIterator.map(_.toString).toList
    // 2. Get values using productIterator
    val values = obj.productIterator.toList
    // 3. Zip together into a Map
    labels.zip(values).toMap

  transparent inline def allCases[T]: List[T] =
    ${allCasesImpl[T]}

  import quoted.*
  private def allCasesImpl[T : Type](using q: Quotes): Expr[List[T]] =
    import q.reflect.*
    val tpe = TypeRepr.of[T]
    val symbol = tpe.termSymbol
    if !symbol.flags.is(Flags.Enum) then
      report.errorAndAbort("allCases is only supported for enums, not " + symbol.name)
    val children = symbol.children.map: child =>
      if child.isTerm && child.flags.is(Flags.Module) then
        Some(Ref(child).asExprOf[T])
      else
        None
    end children
    Expr.ofList(children.collect:
      case Some(child) => child
    )
  end allCasesImpl



  private transparent inline def summonLabelsFromTuple[T <: Tuple]: List[String] =
    inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (t *: ts) => summonInline[ValueOf[t]].value.toString :: summonLabelsFromTuple[ts]

  private transparent inline def tupleToEnumValueList[T <: Tuple, E]: List[E] =
    inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (t *: ts) => summonInline[ValueOf[t]].value.asInstanceOf[E] :: tupleToEnumValueList[ts, E]





end ReflectionUtils