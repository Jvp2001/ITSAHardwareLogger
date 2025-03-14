package org.itsadigitaltrust.macros

import scala.quoted.* 
import scala.annotation.MacroAnnotation
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import scala.annotation.experimental


@Bean
@Component
@experimental
class ScalaSpringBeanComponent extends NoArgConstructor

@experimental
class NoArgConstructor extends MacroAnnotation:
    import NoArgConstructor.*

    override def transform(using Quotes)(definition: quotes.reflect.Definition, companion: Option[quotes.reflect.Definition]): List[quotes.reflect.Definition] = 
        import quotes.reflect.{*, given}
        val tree = definition

        tree match 
        
            case ClassDef(name, ctor, parents, selfOpt, body) =>
                val ctorSym: Symbol = Symbol.newMethod(tree.symbol, "<init>", MethodType(Nil)(_ => Nil, _ => TypeRepr.of[Unit]))
                val ctorDef = DefDef(ctorSym, trees => 
                    val values = ctor.paramss.map: root =>
                        root.params.map: param =>
                            param match
                                case v: ValDef => v.tpt.tpe.getDefaultValue
                                case _ => report.errorAndAbort("Cannot be on type")

                    val termedValues = values.flatMap: value => 
                        value.map: v =>
                            if v.isInstanceOf[String] then                        
                                Expr(v.asInstanceOf[String]).asTerm
                            else if v.isInstanceOf[Int] then
                                Expr(v.asInstanceOf[Int]).asTerm
                            else if v.isInstanceOf[Double] then
                                Expr(v.asInstanceOf[Double]).asTerm
                            else if v.isInstanceOf[Boolean] then
                                Expr(v.asInstanceOf[Boolean]).asTerm
                            else
                               Expr("null").asTerm
                            

                    Some(
                        Apply(Select.unique(New(Inferred(tree.symbol.typeRef)), "<init>"), termedValues))
                )
                val res = List(ClassDef.copy(tree)(name, ctor, parents, selfOpt, body :+ ctorDef))
                println(res)
                res
            case _: Definition => report.errorAndAbort("@ScalaSpringBeanComponent can only be used on classes")



object NoArgConstructor:


    extension[T](using Quotes)(tpe: quotes.reflect.TypeRepr)
        inline def getDefaultValue: Any =
            quotes.reflect.report.info(tpe.typeSymbol.name)
            val name = tpe.termSymbol.fullName
        
            if name == "String" then ""
            else if name == "Int" then 0
            else if name == "Short" then 0
            else if name == "Long" then 0
            else if name == "Float" then 0
            else if name == "Double" then 0
            else if name == "Boolean" then false
            else if name == "<none>" then null
            else if name.startsWith("Some") then None
            else quotes.reflect.report.errorAndAbort(f"Unknown type: ${tpe.termSymbol.name}")
            