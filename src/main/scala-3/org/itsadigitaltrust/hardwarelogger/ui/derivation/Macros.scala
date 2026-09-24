package org.itsadigitaltrust.hardwarelogger.ui.derivation

import org.itsadigitaltrust.hardwarelogger.ui.derivation.annotations.*
import scala.annotation.tailrec
import scala.quoted.*

private[derivation] object Macros:

  inline def findAllAnnotations[A]: List[List[UISchemaAnnotation]] =
    ${ findAllAnnotationsImpl[A] }

  private def findAllAnnotationsImpl[A: Type](using q: Quotes): Expr[List[List[UISchemaAnnotation]]] =
    import q.reflect.*

    val typeSymbol = TypeTree.of[A].symbol

    // 1. Core structural field list (establishes field name order)
    val constructorFields = typeSymbol.caseFields

    // 2. Flattened array of all underlying constructor parameter symbols
    val primaryConstructorParams = typeSymbol.primaryConstructor.paramSymss.flatten

    // 3. Map parameters cleanly by structural position tracking
    val fieldAnnotationsExprs: List[Expr[List[UISchemaAnnotation]]] = constructorFields.zipWithIndex.map: (cField, index) =>

      // Look up symbols using name-fallback OR absolute sequence positioning index
      val paramSymbolOpt = primaryConstructorParams.lift(index).filter(_.name == cField.name)
        .orElse(primaryConstructorParams.find(_.name == cField.name))

      val relatedSymbols = List(
        Some(cField),
        paramSymbolOpt,
        typeSymbol.declarations.find(d => d.name == cField.name)
      ).flatten.distinct

      val allAnnotations = relatedSymbols.flatMap(_.annotations)

      var seenTypes = Set.empty[String]
      val validAnnotExprs = scala.collection.mutable.ListBuffer.empty[Expr[UISchemaAnnotation]]

      allAnnotations.foreach: (tree: Term) =>
        val treeType = tree.tpe
        val typeKey  = treeType.show

        if !seenTypes.contains(typeKey) && (treeType <:< TypeRepr.of[UISchemaAnnotation]) then
          seenTypes += typeKey
          validAnnotExprs += cleanAnnotationTree(tree).asExprOf[UISchemaAnnotation]

      Expr.ofList(validAnnotExprs.toList)

    Expr.ofList(fieldAnnotationsExprs)
  end findAllAnnotationsImpl

  @tailrec
  private def cleanAnnotationTree(using q: Quotes)(tree: q.reflect.Tree): q.reflect.Term =
    import q.reflect.*
    tree match
      case Inlined(_, _, inner) => cleanAnnotationTree(inner)
      case t: Term              => t
      case _                    => report.errorAndAbort(s"Could not process annotation tree structure: ${tree.show}")
end Macros
