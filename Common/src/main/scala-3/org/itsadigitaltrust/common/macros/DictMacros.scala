package org.itsadigitaltrust.common.macros



object DictMacros:

  import org.itsadigitaltrust.common.collections.Dict
  import quoted.*

  transparent inline def apply(inline block: => Unit): Dict = buildFromBlock(block)

    transparent inline def buildFromBlock(inline block: => Unit): Dict =
        ${buildFromBlockImpl('block)}

    private def buildFromBlockImpl(block: Expr[Unit])(using Quotes): Expr[Dict] =
        import quotes.reflect.*
        val x = 42
        def getTypedKeysAndValues(list: List[Statement]): List[(String, TypeTree, Term)] =
            list match
                case Nil =>
                    Nil
                case ValDef(name, typeTree, Some(term)) :: tail =>
                    (name, typeTree, term) :: getTypedKeysAndValues(tail)
                case wrongHead :: tail =>
                    report.errorAndAbort(s"All statements in a Dict initialiser must be initialised val definitions. Found: ${wrongHead.show}")
        end getTypedKeysAndValues

        val info = block.asTerm.underlying match
            case Block(statements, _) =>
              getTypedKeysAndValues(statements)
            case _ => report.errorAndAbort("Paramenter must be a block with only val definitions and no returns")
                Nil
         
        val dictType = info
            .foldLeft(TypeRepr.of[Dict]):
                case (acc, (name, tpt, _)) =>
                    Refinement(
                    parent = acc,
                    name = name,
                    info = tpt.tpe
                    )
                
        val exprTuples = info
        .map:
            case (name, _, term) => 
                '{
                    (${Expr(name)}, ${term.asExpr})
                }
        dictType.asType match
            case '[fullDictType] => 
                ' {Dict.fromUnsafeMap(${Expr.ofList(exprTuples)}.toMap).asInstanceOf[ Dict & fullDictType]}
