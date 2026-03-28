package org.itsadigitaltrust.hardwarelogger.backend

import com.augustnagro.magnum.{Repo, TableInfo}
import org.itsadigitaltrust.hardwarelogger.backend.entities.*
import org.itsadigitaltrust.hardwarelogger.backend.types.{EntityFromEC, ItsaEC}

import scala.quoted.*

/**
  * Compile-time macro helpers for summoning `DbCodec`, `Repo` and `TableInfo`
  * instances based on the concrete `EntityCreator` type `EC`.
  */
object macros:

  inline transparent def summonInline[EC <: HLEntityCreator, E <: HLEntity, Target[_, _]]: Target[EC, E] =
     ${ summonInlineImpl[EC, E, Target] }

  private def summonInlineImpl[EC: Type, E: Type, Target[_, _]: Type](using q: Quotes): Expr[Target[EC, E]] =
    import q.reflect.*

    val ecTpe = TypeRepr.of[EC]

    def exprFor[CC: Type, T: Type]: Expr[Target[CC, T]] =
      Expr.summon[Target[CC, T]].getOrElse:
        report.errorAndAbort(s"No given instance found for Target[${Type.show[CC]}, ${Type.show[T]}]")

    val chosen =
      if ecTpe =:= TypeRepr.of[MemoryCreator] then exprFor[MemoryCreator, Memory]
      else if ecTpe =:= TypeRepr.of[MediaCreator] then exprFor[MediaCreator, Media]
      else if ecTpe =:= TypeRepr.of[DiskCreator] then exprFor[DiskCreator, Disk]
      else if ecTpe =:= TypeRepr.of[InfoCreator] then exprFor[InfoCreator, Info]
      else if ecTpe =:= TypeRepr.of[WipingCreator] then exprFor[WipingCreator, Wiping]
      else if ecTpe =:= TypeRepr.of[HLEntityCreatorWithHardDiskID] then exprFor[WipingCreator, Wiping]
      else report.errorAndAbort(s"No Target mapping found for EntityCreator type: ${ecTpe.show}")

    chosen.asExprOf[Target[EC, E]]

  end summonInlineImpl
    
end macros

