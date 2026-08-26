package org.itsadigitaltrust.hardwarelogger.backend

import org.itsadigitaltrust.hardwarelogger.backend.entities.HLEntityCreator

private[backend] object Macros:
  import quoted.*

  transparent inline def getEntityTypeFromEC[EC <: HLEntityCreator] =
    ${getEntityTypeFromECImpl[EC]}

  private def getEntityTypeFromECImpl[EC <: HLEntityCreator : Type](using q: Quotes): Expr[Any] =
    import q.reflect.*
    
    val ecType = TypeRepr.of[EC]
    val name = ecType.typeSymbol.name.stripSuffix("Creator")
    val targetSymbol = Symbol.requiredClass(name)
    
    TypeIdent(targetSymbol).asExpr

  end getEntityTypeFromECImpl
  
  transparent inline def getFieldValue[I, T](instance: I, fieldName: String) = 
    ${getFieldValueImpl[I, T]('instance, 'fieldName)}
    
  private def getFieldValueImpl[I : Type, T : Type](instance: Expr[I], fieldName: Expr[String])(using q: Quotes): Expr[T] =
    import q.reflect.*
    val fieldType = TypeRepr.of[T]
    val instanceType = TypeRepr.of[I]
    
    val fieldSymbol = instance.asTerm.symbol.fieldMember(fieldName.valueOrAbort)
    if fieldSymbol.isNoSymbol then
      report.errorAndAbort(s"Failed to find '$fieldName' in ${instance.show}")
      
    Select(Ref(instance.asTerm.symbol), fieldSymbol).asExprOf[T]
  end getFieldValueImpl
  
    
end Macros
