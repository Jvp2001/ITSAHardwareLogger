package org.itsadigitaltrust.hardwarelogger



object Macros:
  import quoted.*
  transparent inline def getPrivateMemberField[T, R](inline instance: T, inline name: String): R =
    ${getPrivateMemberFieldImpl[T, R]('instance, '{name})}

  end getPrivateMemberField

  def getPrivateMemberFieldImpl[T : Type, R : Type](t: Expr[T], fieldName: Expr[String])(using q: Quotes): Expr[R] =
    import q.reflect.*
    val instanceTerm: Term = t.asTerm
    val instanceSymbol = instanceTerm.symbol

    val tpe = TypeRepr.of[T]
    val nameToFind = fieldName.valueOrAbort
    val symbol = tpe.typeSymbol

    val fieldTree = symbol.fieldMembers.find(_.name == nameToFind).getOrElse(report.errorAndAbort("Cannot find field " + nameToFind))
//      case Some(f) if f.isInstanceOf[ValDef] => f.asInstanceOf[ValDef]
//      case Some(f) =>
//        report.errorAndAbort(s"Member '$nameToFind' exists, but is not a valid field (ValDef)")
//      case None =>
//        report.errorAndAbort(s"Field '$nameToFind' not found in ${tpe.show}. Available: ${symbol.fieldMembers.map(_.name).mkString(", ")}")
//    
    end fieldTree

    val fieldType = fieldTree.tree.match
      case ValDef(_, tpt, _) => tpt.tpe
      case null => report.errorAndAbort("Unexpected tree structure!")
      
    if !(fieldType <:< TypeRepr.of[R]) then
      report.errorAndAbort(s"""Field '$nameToFind' is of type ${fieldType.show}, but expected ${TypeRepr.of[R].show}""")
      
    val selectTerm = Select(instanceTerm, fieldTree)

    selectTerm.asExprOf[R]
    
  end getPrivateMemberFieldImpl
end Macros