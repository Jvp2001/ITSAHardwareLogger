package org.itsadigitaltrust.hardwarelogger.services.database

import com.augustnagro.magnum.{DbCodec, DbCon, DbTx}
import org.itsadigitaltrust.common.Operators.|>
import org.itsadigitaltrust.hardwarelogger.backend.HLTableInfo
import org.itsadigitaltrust.hardwarelogger.backend.entities.HLEntityWithSomeKindOfID
import org.itsadigitaltrust.hardwarelogger.backend.repos.HLRepoWithID
import org.itsadigitaltrust.hardwarelogger.models.HLModel
import org.itsadigitaltrust.hardwarelogger.services.HardwareGrabberService
import ox.either.ok

import scala.quoted.{Expr, Quotes, Type}

trait FindAllByIDSuffixedWith[M <: HLModel]:
  def find(model: M, suffix: String)(using
                           dbTx: DbTx,
                           hardwareGrabberService: HardwareGrabberService
  ): Either[HLDatabaseServiceExceptionBase, Vector[M]]


trait FindAllSuffixedWithErrorNumber[M <: HLModel]:
  def find(model: M)(using dbTx: DbTx, hardwareGrabberService: HardwareGrabberService): Either[HLDatabaseServiceExceptionBase, Vector[M]]


object FindAllSuffixedWithErrorNumber:
  given findAllErrorCount[M <: HLModel : FindAllByIDSuffixedWith as finder]: FindAllSuffixedWithErrorNumber[M] with
    override def find(model: M)(using dbTx: DbTx, hardwareGrabberService: HardwareGrabberService): Either[HLDatabaseServiceExceptionBase, Vector[M]] =
      ox.either:  
        finder.find(model, "-E[0-9]%").ok()
    

//
//  transparent inline def generateFindByIDSuffixedWithInstance[R <: HLRepoWithID[?, E], E <: HLEntityWithSomeKindOfID : DbCodec, M <: HLModel](databaseService: HLDatabaseService)(using converter: FromEntity[E, M]) =
//    ${ generateFindByIDSuffixedWithInstanceImpl[R, E, M]('databaseService)('{ converter.apply }) }
//
//  private def generateFindByIDSuffixedWithInstanceImpl[R <: HLRepoWithID[?, E] : Type, E <: HLEntityWithSomeKindOfID : Type, M <: HLModel : Type](databaseService: Expr[HLDatabaseService])(f: Expr[E => M])(using q: Quotes): Expr[FindAllSuffixedWithErrorNumber[M]] =
//    import q.reflect.*
//
//    val entityType = TypeRepr.of[E]
//    val entityTypeSymbol = entityType.typeSymbol
//
//    // 1. Get the Companion Object Symbol
//    val reposSymbol = Symbol.requiredModule("org.itsadigitaltrust.hardwarelogger.backend.repos")
//
//    if reposSymbol.isNoSymbol then
//      report.errorAndAbort(s"No companion object found for ${Type.show[E]}. Cannot locate repository instance.")
//
//    // 2. Calculate the expected field name
//    val entityRepoInstanceName = entityTypeSymbol.name.toLowerCase + "Repo"
//
//    // 3. Look up the field ON THE COMPANION CLASS
//    val repoFieldSymbol = reposSymbol.fieldMember(entityRepoInstanceName)
//
//    // 4. Handle MatchError by explicitly checking for NoSymbol
//    if repoFieldSymbol.isNoSymbol then
//      val availableFields = reposSymbol.fieldMembers.map(_.name).mkString(", ")
//      report.errorAndAbort(
//        s"Field '$entityRepoInstanceName' not found in companion object of ${Type.show[E]}.\n" +
//          s"Available fields: $availableFields"
//      )
//
//    // 5. Construct the selection: CompanionObject.field
//    val repoInstanceTerm = Select(Ref(reposSymbol), repoFieldSymbol)
//
//    // Summon codec
//    val codecExpr = Expr.summon[DbCodec[E]] match
//      case Some(codec) => codec
//      case None => report.errorAndAbort("Could not find implicit DbCodec for type " + Type.show[E])
//
//    // --- FIX ENDS HERE ---
//
//    '{
//      new FindAllByIDSuffixedWith[M]:
//        override def find(suffix: String)(using
//                                          dbTx: DbTx,
//                                          hardwareGrabberService: HardwareGrabberService
//        ): Either[HLDatabaseServiceExceptionBase,Vector[M]] =
//          import ox.either.{ok ,fail}
//          ox.either:
//            // Provide the DbCon locally for this method call
//            given dbCon: DbCon = dbTx.asInstanceOf[DbCon]
//
//            given codec: DbCodec[E] = ${ codecExpr.asExprOf[DbCodec[E]] }
//
//
//            val result: Vector[Option[M]] = ${ databaseService.asExprOf[HLDatabaseService] }.transact:
//              ${ repoInstanceTerm.asExprOf[R] }.findAllByIDSuffixedWith(suffix)
//
//            if result.isEmpty || result.exists(_.isEmpty) then
//              HLDatabaseServiceException.noEntriesFound("No entries found for suffix: " + suffix).fail()
//              
//            result.map(_.ok())
//                
//              
//    }
//  end generateFindByIDSuffixedWithInstanceImpl
end FindAllSuffixedWithErrorNumber
