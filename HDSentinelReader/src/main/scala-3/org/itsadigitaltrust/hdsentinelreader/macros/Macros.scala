package org.itsadigitaltrust.hdsentinelreader.macros

import scala.quoted.*
import scala.annotation.Annotation
import org.itsadigitaltrust.hdsentinelreader.macros.MacroUtils.getFields
/**
  *   def createPersonAST(using Quotes): Expr[Person] =
    import quotes.reflect.*

    // Get the type symbol for Person
    val personType = TypeRepr.of[Person]
    val personSymbol = personType.typeSymbol

    // Ensure the type has a primary constructor
    val primaryConstructor = personSymbol.primaryConstructor
    if primaryConstructor == Symbol.noSymbol then
      report.errorAndAbort(s"Type ${personSymbol.name} does not have a primary constructor")

    // Create named arguments for the constructor
    val nameArg = NamedArg("name", Expr("John Doe").asTerm) // name = "John Doe"
    val ageArg = NamedArg("age", Expr(20).asTerm)          // age = 20

    // Create the Apply node for the constructor call
    val constructorCall = Apply(
      Select(New(TypeTree.of[Person]), primaryConstructor),
      List(nameArg, ageArg)
    )
*/
final case class ParameterInfo(name: String, `type`: String, value: Any)
extension(params: Seq[ParameterInfo])
  def get[T](name: String): T =


final case class AnnotationInfo(name: String, args: Seq[ParameterInfo])

final case class FieldInfo(field: String, `type`: String, annotations: List[AnnotationInfo]):
  def hasAnnotation(name: String): Boolean = 
    annotations.exists(_.name.endsWith(name))

  def apply(name: String): Option[AnnotationInfo] =
    annotations.find(_.name.endsWith(name))

extension (fields: List[FieldInfo])
  def withAnnotation(name: String): List[FieldInfo] = 
    fields.filter: field =>
      field.hasAnnotation(name)
      
// Define ToExpr instances for custom types
given ToExpr[ParameterInfo] with
  def apply(p: ParameterInfo)(using Quotes): Expr[ParameterInfo] =
    '{ ParameterInfo(${Expr(p.name)}, ${Expr(p.`type`)}) }

given ToExpr[AnnotationInfo] with
  def apply(a: AnnotationInfo)(using Quotes): Expr[AnnotationInfo] =
    '{ AnnotationInfo(${Expr(a.name)}, ${Expr.ofSeq(a.args.map(summon[ToExpr[ParameterInfo]].apply))}) }

given ToExpr[FieldInfo] with
  def apply(f: FieldInfo)(using Quotes): Expr[FieldInfo] =

    '{ FieldInfo(${Expr(f.field)}, ${Expr(f.`type`)}, ${Expr.ofList(f.annotations.map(summon[ToExpr[AnnotationInfo]].apply))}) }
object MacroUtils:

  def getTypeSymbol[T: Type](using Quotes): quotes.reflect.Symbol =
    import quotes.reflect.*
    TypeRepr.of[T].typeSymbol

  def getFields[T: Type](using Quotes): Seq[FieldInfo] =
    import quotes.reflect.*

    val typeSymbol = getTypeSymbol[T]

    typeSymbol
      .primaryConstructor
      .paramSymss
      .flatten
      .map: sym =>
        val fieldName = sym.name
        val fieldType = sym.tree match
          case v: ValDef => v.tpt.tpe.show
          case _ => "Unknown"
        val annotations = sym.annotations.map: annotation =>
          val args = annotation match
            case Apply(_, params) =>  
              params.map:
                case NamedArg(name, Literal(constant)) =>
                  ParameterInfo(name, constant.show, constant.value)
                case Literal(constant) =>
                  ParameterInfo("value", constant.show, constant.value)
                case _ =>
                  ParameterInfo("unknown", "unknown", "unknown")            
            case _ => List()

          AnnotationInfo(annotation.symbol.name, args)
        
        FieldInfo(fieldName, fieldType, annotations)
      




object Macros:

  inline def getFields[T] =
    ${ getFieldsImpl[T] }

  private def getFieldsImpl[T: Type](using Quotes): Expr[Seq[FieldInfo]] =
    import quotes.reflect.*
    Expr.ofSeq(MacroUtils.getFields[T].map(Expr[FieldInfo]))
    
  
    inline def constructType[T](fields:)


// val result = typeSymbol.declaredFields.map: field =>
//   Expr(typeSymbol.name, field.name, field.annotations.size)
// Expr.ofList(result)

export Macros.*