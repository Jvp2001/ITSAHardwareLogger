package org.itsadigitaltrust.common.configformats

import scala.quoted.*

object AnnotationExtractor:
  /**
   * Deeply inspects an AST Term to extract a compile-time static String literal.
   */
  def extractStringFromTerm(using Quotes)(term: quotes.reflect.Term): String =
    import quotes.reflect.*
    term match
      case Literal(StringConstant(value)) => value
      case Literal(c: Constant) =>
        c.value match
          case str: String => str
          case _           => ""
      case Apply(_, args) =>
        args.map(extractStringFromTerm).find(_.nonEmpty).getOrElse("")
      case NamedArg(_, value) =>
        extractStringFromTerm(value)
      case Block(_, expr) =>
        extractStringFromTerm(expr)
      case Inlined(_, _, expr) =>
        extractStringFromTerm(expr)
      case Typed(expr, _) =>
        extractStringFromTerm(expr)
      case Select(qual, _) =>
        extractStringFromTerm(qual)
      case _ =>
        ""

  /**
   * Finds an annotation of type `annType` on a Symbol or its corresponding constructor parameter,
   * returning the extracted String value if present.
   */
  def findAnnotationValue(using Quotes)(
    fieldSymbol: quotes.reflect.Symbol,
    allCtorParams: List[quotes.reflect.Symbol],
    annType: quotes.reflect.TypeRepr
  ): Option[String] =
    import quotes.reflect.*
    val correspondingParam = allCtorParams.find(_.name == fieldSymbol.name)
    val paramAnnotations = correspondingParam.map(_.annotations).getOrElse(Nil)
    val combinedAnnotations = fieldSymbol.annotations ++ paramAnnotations

    combinedAnnotations.find(_.tpe <:< annType).map(extractStringFromTerm)
end AnnotationExtractor


private[configformats] object ConfigConversionMacros:

  inline def mappingFunction[T]: T => CaseClassConfigInfo =
    ${ impl[T] }

  private def impl[T: Type](using Quotes): Expr[T => CaseClassConfigInfo] =
    import quotes.reflect.*

    val configGroupPrefixAnn = TypeRepr.of[ConfigGroupPrefix]
    val configKeyAnn = TypeRepr.of[ConfigKey]
    val commentAnn = TypeRepr.of[ConfigComment]

    val tpe = TypeRepr.of[T]
    val tpeSymbol = tpe.typeSymbol

    if !tpeSymbol.isClassDef || !tpeSymbol.flags.is(Flags.Case) then
      report.errorAndAbort(s"Type ${tpe.show} is not a case class")

    // Extract the group prefix from the class definition annotation term
    val groupPrefixExpr: Expr[String] =
      tpeSymbol.annotations.find(_.tpe <:< configGroupPrefixAnn)
        .map(term => Expr(AnnotationExtractor.extractStringFromTerm(term)))
        .getOrElse(Expr(""))

    val caseFields = tpeSymbol.caseFields
    val allCtorParams = tpeSymbol.primaryConstructor.paramSymss.flatten

    // FIX: Perform the extraction loops outside the '{ (instance: T) => ... }' quote boundary.
    // This keeps the Quotes path alignment completely consistent.
    val fieldsData: List[(String, Expr[String], Expr[String])] = caseFields.map: field =>
      val name = field.name
      val commentStr = AnnotationExtractor.findAnnotationValue(field, allCtorParams, commentAnn).getOrElse("")
      val keyStr = AnnotationExtractor.findAnnotationValue(field, allCtorParams, configKeyAnn).getOrElse("")
      (name, Expr(keyStr), Expr(commentStr))

    '{ (instance: T) =>
      val fieldContents = ${
      val fieldContentExprs: List[Expr[CaseClassFieldInfo]] = fieldsData.map: (name, keyExpr, commentExpr) =>
        val valueExpr: Expr[String] =
          val instanceTerm = '{ instance }.asTerm
          val fieldTerm = Select.unique(instanceTerm, name)
          fieldTerm.tpe.asType match
            case '[any] =>
              '{ ${fieldTerm.asExprOf[any]}.toString }

        '{ CaseClassFieldInfo(${Expr(name)}, $keyExpr, $valueExpr, $commentExpr) }

      Expr.ofList(fieldContentExprs)
    }

      CaseClassConfigInfo($groupPrefixExpr, fieldContents)
    }
end ConfigConversionMacros
