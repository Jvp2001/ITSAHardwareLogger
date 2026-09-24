package org.itsadigitaltrust.common.processes.tlp

import scala.compiletime.{constValue, erasedValue, summonInline}
import scala.deriving.Mirror

trait TlpDecoder[A]:
  def decode(source: Map[String, String]): Option[A]

object TlpDecoder:
  // 🌟 Helper factory method to instantiate a decoder instance cleanly
  def apply[A](f: Map[String, String] => Option[A]): TlpDecoder[A] =
    (source: Map[String, String]) => f(source)

  private def normalizeKey(s: String): String =
    s.replaceAll("([A-Z])", " $1").toLowerCase.trim

  // Helper helper to strip out unit markers like "[%]", "[V]", or "mAh" from raw strings
  private def cleanNumericString(s: String): String =
    s.replaceAll("\\[.*?\\]", "").replaceAll("[a-zA-Z]", "").trim

  // Primitives (Standard Basic Types)
  given TlpDecoder[String] with
    def decode(source: Map[String, String]): Option[String] = Some(source.getOrElse("unknown", ""))

  given TlpDecoder[Int] with
    def decode(source: Map[String, String]): Option[Int] =
      source.get("unknown").flatMap(s => cleanNumericString(s).toIntOption)

  given TlpDecoder[Double] with
    def decode(source: Map[String, String]): Option[Double] =
      source.get("unknown").flatMap(s => cleanNumericString(s).toDoubleOption)

  given optionStringDecoder: TlpDecoder[Option[String]] with
    def decode(source: Map[String, String]): Option[Option[String]] =
      Some(source.get("unknown").filter(_.trim.nonEmpty))

  given optionIntDecoder: TlpDecoder[Option[Int]] with
    def decode(source: Map[String, String]): Option[Option[Int]] =
      Some(source.get("unknown").flatMap(s => cleanNumericString(s).toIntOption))

  given optionDoubleDecoder: TlpDecoder[Option[Double]] with
    def decode(source: Map[String, String]): Option[Option[Double]] =
      Some(source.get("unknown").flatMap(s => cleanNumericString(s).toDoubleOption))

  import quoted.*

  // The macro splice remains the absolute top-level expression on the RHS.
  inline given derived[A](using m: Mirror.ProductOf[A]): TlpDecoder[A] =
    ${ macroImpl[A]('{ m }) }

  private def macroImpl[A: Type](mirrorExpr: Expr[Mirror.ProductOf[A]])(using Quotes): Expr[TlpDecoder[A]] =
    import quotes.reflect.*

    val mirrorType = TypeRepr.of[A]
    val classSymbol = mirrorType.typeSymbol
    val fields = classSymbol.caseFields
    val annotationSymbol = TypeRepr.of[TlpProperty].typeSymbol

    '{
      TlpDecoder: source =>
        ${
          val fieldExprs: List[Expr[Option[Any]]] = fields.map: fieldSymbol =>
            val fieldName = fieldSymbol.name
            val fieldType = mirrorType.memberType(fieldSymbol)

            val customNameOpt: Option[String] = fieldSymbol.getAnnotation(annotationSymbol).flatMap:
              case Apply(Select(New(_), _), List(Literal(StringConstant(name)))) => Some(name)
              case _ => None

            fieldType.asType match
              case '[t] =>
                Expr.summon[TlpDecoder[t]] match
                  case Some(decoder) =>
                    '{
                      val rawLabel = ${ Expr(fieldName) }
                      val customLabelOpt = ${ Expr(customNameOpt) }
                      val lookupKey = customLabelOpt.getOrElse(TlpDecoder.normalizeKey(rawLabel))

                      val lookupVal = source.get(lookupKey)
                        .orElse(source.get(rawLabel))
                        .orElse(source.find(_._1.toLowerCase.endsWith(lookupKey)).map(_._2))

                      val isOptionType = ${ Expr(TypeRepr.of[t] <:< TypeRepr.of[Option[?]]) }

                      // 🌟 FIX: Actively intercept missing keys for Option fields at the macro code block level
                      if isOptionType then
                        lookupVal match
                          case Some(str) if str.trim.nonEmpty =>
                            $decoder.decode(Map("unknown" -> str))
                          case _ =>
                            // Key is either missing or empty -> assign a clean Option wrapper payload
                            Some(None)
                      else
                        // Required fields continue to fail the overall object construction if missing
                        lookupVal.flatMap: str =>
                          $decoder.decode(Map("unknown" -> str))
                    }
                  case None =>
                    report.errorAndAbort(s"Could not summon a TlpDecoder for type ${fieldType.show} on field $fieldName")

          val sequenceExpr: Expr[Option[List[Any]]] = fieldExprs.foldRight('{ Option(List.empty[Any]) }): (itemExpr, accExpr) =>
            '{
              for
                item <- $itemExpr
                acc <- $accExpr
              yield item :: acc
            }

          '{
            $sequenceExpr.map: listData =>
              val tuple = Tuple.fromArray(listData.toArray).asInstanceOf[Product]
              $mirrorExpr.fromProduct(tuple)
          }
        }
    }
  end macroImpl
  

end TlpDecoder
