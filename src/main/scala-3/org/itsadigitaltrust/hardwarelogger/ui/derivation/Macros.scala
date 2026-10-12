package org.itsadigitaltrust.hardwarelogger.ui.derivation

import org.itsadigitaltrust.hardwarelogger.ui.derivation.annotations.*
import scala.annotation.tailrec
import scala.quoted.*

private[derivation] object Macros:

  inline def findAllAnnotations[A]: List[List[UISchemaAnnotation]] =
    ${ findAllAnnotationsImpl[A] }

  /**
   * Compiles an explicit mapping of a parameterized enum case's inner constructor parameters,
   * returning a list of tuples containing (FieldName, ClassTypeReprString).
   */
  inline def extractEnumCaseFields[A]: List[(String, String)] =
    ${ extractEnumCaseFieldsImpl[A] }

  private def extractEnumCaseFieldsImpl[A: Type](using q: Quotes): Expr[List[(String, String)]] =
    import q.reflect.*
    val typeSymbol = TypeTree.of[A].symbol

    // Target the primary fields inside the case class variant (even if packaged inside an Enum parent)
    val constructorFields = typeSymbol.caseFields
    val pairs = constructorFields.map { cField =>
      val nameExpr = Expr(cField.name)

      // Determine field type structural signature safely at compile time
      val fieldTypeStr = Expr(cField.tree.asInstanceOf[ValDef].tpt.tpe.show)
      '{ ($nameExpr, $fieldTypeStr) }
    }
    Expr.ofList(pairs)
  end extractEnumCaseFieldsImpl

  private def findAllAnnotationsImpl[A: Type](using q: Quotes): Expr[List[List[UISchemaAnnotation]]] =
    import q.reflect.*
    val typeSymbol = TypeTree.of[A].symbol

    if (typeSymbol.flags.is(Flags.Sealed) || typeSymbol.flags.is(Flags.Enum)) && !typeSymbol.flags.is(Flags.Case) then
      val children = typeSymbol.children
      val caseAnnotationsExprs: List[Expr[List[UISchemaAnnotation]]] = children.map: childSymbol =>
        val allAnnotations = childSymbol.annotations
        var seenTypes = Set.empty[String]
        val validAnnotExprs = scala.collection.mutable.ListBuffer.empty[Expr[UISchemaAnnotation]]

        allAnnotations.foreach: (tree: Term) =>
          val treeType = tree.tpe
          val typeKey  = treeType.show
          if !seenTypes.contains(typeKey) && (treeType <:< TypeRepr.of[UISchemaAnnotation]) then
            seenTypes += typeKey
            validAnnotExprs += cleanAnnotationTree(tree).asExprOf[UISchemaAnnotation]
        Expr.ofList(validAnnotExprs.toList)
      Expr.ofList(caseAnnotationsExprs)
    else
      val constructorFields = typeSymbol.caseFields
      val primaryConstructorParams = typeSymbol.primaryConstructor.paramSymss.flatten

      val fieldAnnotationsExprs: List[Expr[List[UISchemaAnnotation]]] = constructorFields.zipWithIndex.map: (cField, index) =>
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
