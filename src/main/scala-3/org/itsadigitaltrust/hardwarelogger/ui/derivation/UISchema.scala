package org.itsadigitaltrust.hardwarelogger.ui.derivation

import org.itsadigitaltrust.hardwarelogger.ui.derivation.annotations.Ignore

import scala.compiletime.{constValue, constValueTuple, erasedValue, summonAll, summonInline}
import scala.deriving.Mirror

trait UISchema[A]:
  def buildEditor(value: A): (List[FieldUI], () => A)

  final def apply(value: A): (List[FieldUI], () => A) = buildEditor(value)
end UISchema

object UISchema:

  /**
   * 💡 WHY DO WE NEED THIS TYPE ALIAS?
   *
   * In Scala 3, type identifiers extracted via abstract mirrors (like `m.MirroredElemTypes`)
   * are treated by the compiler as opaque path-dependent types. If you pass them directly into
   * an `inline match`, the macro compiler panics with a "cannot reduce inline match" error.
   *
   * By routing the type through this `AsTuple` proxy type alias, we force the compiler's
   * type checker to strip away the opaque path wrapper and evaluate the underlying type
   * structurally as a true, unrollable `Tuple` sequence.
   */
  type AsTuple[T <: Tuple] = T

  // Recursively summons individual UI factories for each element in the structural type tuple
  private inline def summonDecoders[T <: Tuple]: List[UIControlFactory[?]] =
    inline erasedValue[AsTuple[T]] match
      case _: EmptyTuple => Nil
      case _: (t *: ts)  => summonInline[UIControlFactory[t]] :: summonDecoders[ts]

  // Recursively extracts literal string representations of case class field names
  private inline def summonLabels[T <: Tuple]: List[String] =
    inline erasedValue[AsTuple[T]] match
      case _: EmptyTuple => Nil
      case _: (t *: ts)  => constValue[t].toString :: summonLabels[ts]

  inline given derived[A](using m: Mirror.ProductOf[A]): UISchema[A] =
    new UISchema[A]:
      def buildEditor(value: A): (List[FieldUI], () => A) =

        /**
         * 💡 WHY DO WE USE THE '& Tuple' INTERSECTION TYPE HERE?
         *
         * `m.MirroredElemLabels` and `m.MirroredElemTypes` are abstract type definitions provided by
         * the `Mirror` trait. Even though we (and the compiler) know they evaluate to tuples, their
         * high-level type signatures do not explicitly inherit from `scala.Tuple`.
         *
         * By applying an Intersection Type (`m.MirroredElemLabels & Tuple`), we explicitly prove
         * to the type-checker at the call site that these dependencies safely satisfy the `[T <: Tuple]`
         * invariant constraint required by our inline unrolling methods.
         */
        val labels = summonLabels[m.MirroredElemLabels & Tuple]

        val factories = summonDecoders[m.MirroredElemTypes & Tuple].asInstanceOf[List[UIControlFactory[Any]]]

        // Extract runtime configuration values and match them alongside macro-parsed metadata annotations
        val runtimeValues = value.asInstanceOf[Product].productIterator.toList
        val allAnnots = Macros.findAllAnnotations[A]


        // 1. First assemble all possible fields alongside their compiled metadata
        val allFields = labels.zip(runtimeValues).zip(factories).zip(allAnnots).map:
          case (((name, fieldValue), factory), annots) =>
            val managedField = factory.createField(fieldValue, annots)
            FieldUI(name, managedField, annots)
        

        // 2. Filter out any fields marked with @Ignore using your new helper method
        val activeFields = allFields.filterNot(_.hasAnnotation[Ignore])

        // 3. The reconstructor remains structurally bound to allFields to correctly satisfy
        // the original positional constructor indices needed by m.fromProduct
        val reconstructor = () =>
          val evaluatedArgs = allFields.map(_.field.flushValue())
          m.fromProduct(Tuple.fromArray(evaluatedArgs.toArray).asInstanceOf[m.MirroredElemTypes])

        (activeFields, reconstructor)

end UISchema
