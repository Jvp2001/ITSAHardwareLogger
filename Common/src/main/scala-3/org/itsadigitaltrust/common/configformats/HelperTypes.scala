package org.itsadigitaltrust.common.configformats


import quoted.*

import scala.quoted.{Expr, FromExpr, Quotes, ToExpr, Varargs}


final case class CaseClassFieldInfo(name: String, keyName: String, value: String, comment: String)

object CaseClassFieldInfo:
  given FromExpr[CaseClassFieldInfo] with
    def unapply(expr: Expr[CaseClassFieldInfo])(using Quotes): Option[CaseClassFieldInfo] =
      expr match
        case '{ CaseClassFieldInfo(${ Expr(name) }, ${ Expr(keyName) }, ${ Expr(value) }, ${ Expr(comment) }) } =>
          Some(CaseClassFieldInfo(name, keyName, value, comment))
        case _ =>
          None
    end unapply
  end given

  // FIX 1: Moved this OUTSIDE of the FromExpr block so it is visible in the companion scope
  given ToExpr[CaseClassFieldInfo] with
    override def apply(x: CaseClassFieldInfo)(using Quotes): Expr[CaseClassFieldInfo] =
      '{ CaseClassFieldInfo(${ Expr(x.name) }, ${ Expr(x.keyName) }, ${ Expr(x.value) }, ${ Expr(x.comment) }) }

end CaseClassFieldInfo


private[configformats] final case class CaseClassConfigInfo(groupPrefix: String, fields: List[CaseClassFieldInfo])

object CaseClassConfigInfo:
  given fromExpr: FromExpr[CaseClassConfigInfo] with
    override def unapply(expr: Expr[CaseClassConfigInfo])(using Quotes): Option[CaseClassConfigInfo] =
      import quotes.reflect.*
      expr match
        case '{ CaseClassConfigInfo(${ Expr(groupPrefix) }, List(${ Varargs(fieldExprs) } *)) } =>
          val contentExtractor = summon[FromExpr[CaseClassFieldInfo]]
          val parsedFields: Option[List[CaseClassFieldInfo]] =
            fieldExprs.toList.map(contentExtractor.unapply).forall(_.isDefined) match
              case true => Some(fieldExprs.toList.flatMap(contentExtractor.unapply))
              case false => None
          parsedFields.map(fields => CaseClassConfigInfo(groupPrefix, fields))
        case _ =>
          None
  end fromExpr

  // FIX 2: Changed target type from CaseClassFieldInfo to CaseClassConfigInfo
  given toExpr: ToExpr[CaseClassConfigInfo] with
    def apply(config: CaseClassConfigInfo)(using Quotes): Expr[CaseClassConfigInfo] =
      val prefixExpr = Expr(config.groupPrefix)

      // Map elements using the implicit ToExpr[CaseClassFieldInfo] provided above
      val fieldExprs: List[Expr[CaseClassFieldInfo]] = config.fields.map(Expr(_))

      // Bundle the list of expressions into an Expr[List[CaseClassFieldInfo]]
      val fieldsListExpr: Expr[List[CaseClassFieldInfo]] = Expr.ofList(fieldExprs)

      '{ CaseClassConfigInfo($prefixExpr, $fieldsListExpr) }
  end toExpr

end CaseClassConfigInfo
