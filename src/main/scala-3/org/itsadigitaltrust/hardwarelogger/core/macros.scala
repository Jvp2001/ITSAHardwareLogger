package org.itsadigitaltrust.hardwarelogger.core


object macros:
    import scala.quoted.* 
    import org.itsadigitaltrust.hardwarelogger.views.View
    inline def makeViewModelSeq[T]: List[View[?]] =
        ${ makeViewModelSeqImpl[T] }

    private def makeViewModelSeqImpl[T: Type](using q:Quotes): Expr[List[View[?]]] =
        import q.reflect.*
        // !. Get the type symbol of T
        val typeRepr = TypeRepr.of[T]
        val typeSymbol = typeRepr.typeSymbol

        // 2. Extract all members of T that are subtypes of View
        val members: List[Symbol] = typeSymbol.declarations
            .filter: member =>
                val memberType: TypeRepr = member.typeRef
                memberType <:< TypeRepr.of[View[?]]
        end members
        val viewModels: List[(view:Symbol, viewModel:Symbol)] = members.map: member =>
            (member, member.fieldMember("viewModel"))

        // 3. Create an expression for each member
        val viewTerms = viewModels.map: member =>
          Select.unique(Ref(member.view), member.viewModel.name)
        end viewTerms

        val exprs  = viewTerms.map(_.asExprOf[View[?]])
        Expr.ofList[View[?]](exprs)
    end makeViewModelSeqImpl
end macros
