package org.itsadigitaltrust.common.configformats

import scala.quoted.*
import java.util.Properties
import scala.deriving.Mirror

private[configformats] object PropertyReaderMacros:
  import org.itsadigitaltrust.common.configformats.PropertyReader
  inline def deriveReader[A]: PropertyReader[A] =
    ${ deriveReaderImpl[A] }

  def deriveReaderImpl[A: Type](using Quotes): Expr[PropertyReader[A]] =
    import quotes.reflect.*

    val configGroupPrefixAnn = TypeRepr.of[ConfigGroupPrefix]
    val configKeyAnn = TypeRepr.of[ConfigKey]

    val tpe = TypeRepr.of[A]
    val tpeSymbol = tpe.typeSymbol

    if !tpeSymbol.isClassDef || !tpeSymbol.flags.is(Flags.Case) then
      report.errorAndAbort(s"Type ${tpe.show} is not a case class")

    val mirrorExpr = Expr.summon[Mirror.ProductOf[A]].getOrElse:
      report.errorAndAbort(s"Could not find a Mirror.ProductOf instance for case class ${tpe.show}")

    val classPrefix = tpeSymbol.annotations.find(_.tpe <:< configGroupPrefixAnn)
      .map(AnnotationExtractor.extractStringFromTerm)
      .getOrElse("")

    val caseFields = tpeSymbol.caseFields
    val ctorParams = tpeSymbol.primaryConstructor.paramSymss.flatten

    val fieldsSchema: List[(String, String, TypeRepr)] = caseFields.map: field =>
      val name = field.name
      val defaultKebabKey = NamingConventionUtils.camelToKebab(name)
      val explicitKey = AnnotationExtractor.findAnnotationValue(field, ctorParams, configKeyAnn)
      val finalKeyName = explicitKey.getOrElse(defaultKebabKey)

      (name, finalKeyName, field.tree.asInstanceOf[ValDef].tpt.tpe)

    '{
      new PropertyReader[A]:
        def read(props: Properties, runtimePrefix: String): Either[String, A] =
          val basePrefix =
            if runtimePrefix.nonEmpty then runtimePrefix
            else ${ Expr(classPrefix) }

          val results = ${
            val steps: List[Expr[Either[String, Any]]] = fieldsSchema.map: (name, key, fieldTpe) =>
              val liftedKeyExpr = Expr(key)

              fieldTpe.asType match
                case '[t] =>
                  val summonReader = Expr.summon[PropertyReader[t]].getOrElse:
                    report.errorAndAbort(s"Could not locate an implicit PropertyReader instance for type ${fieldTpe.show}")

                  '{
                    val absoluteKey = if (basePrefix.nonEmpty) s"$basePrefix.${$liftedKeyExpr}" else $liftedKeyExpr
                    $summonReader.read(props, absoluteKey).map(_.asInstanceOf[Any])
                  }

            Expr.ofList(steps)
          }

          results.foldLeft[Either[String, List[Any]]](Right(Nil)): (acc, item) =>
            for
              list <- acc
              elem <- item
            yield list :+ elem
          .map: parsedValues =>
            $mirrorExpr.fromProduct(Tuple.fromArray(parsedValues.toArray)).asInstanceOf[A]
    }
end PropertyReaderMacros
