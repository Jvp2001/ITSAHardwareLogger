package org.itsadigitaltrust.hardwarelogger.backend.database

import com.augustnagro.magnum.{DbCodec, DbCon}
import org.itsadigitaltrust.common.Operators.{?, ??, |>}
import org.itsadigitaltrust.hardwarelogger.backend.{HLDBErrorCode, HLDBTransactionResult}
import org.itsadigitaltrust.hardwarelogger.backend.entities.{HLEntityCreatorWithSomeKindOfID, HLEntityWithSomeKindOfID, InfoCreator}
import org.itsadigitaltrust.hardwarelogger.backend.repos.{HLRepoWithID, HLRepoWithSerial}
import ox.either.{fail, ok}

import java.sql.Timestamp
import java.time.OffsetDateTime
import scala.reflect.{ClassTag, ensureAccessible}
import scala.compiletime.{deferred, summonInline}

private trait UpdateAndInsertHelpers[EC <: HLEntityCreatorWithSomeKindOfID : {ClassTag}]:
  given mapper: EntityCreatorToEntityMapper[EC] = deferred

  given repo: HLRepoWithID[EC, ? <: HLEntityWithSomeKindOfID] = deferred

  def serialNumberToUseForFinding(ec: EC): Option[String] = None
      
  

  def insert(ec: EC)(using oldItsaID: Option[String])(using DbCon): HLDBTransactionResult[Unit] =
    ox.either:
      val result = repo.findByItsaID(ec.getItsaID)
      val currentItsaID = ec.getItsaID ?? HLDBErrorCode.UnknownError("No Itsa ID").fail()
      if result.isEmpty then
        if oldItsaID.isDefined && currentItsaID != oldItsaID.get then
        repo.insert(ec)
      end if
  end insert


  /**
   * @note All entity creators must have the same ITSA ID to be updated successfully.
   *       All entity creators will be marked with the same error number.
   *       This is because some tables, like memory, can have multiple rows with the same ITSA ID.
   * @param ecs
   * @param oldItsaID
   * @param x$3
   * @return
   */
  def update(ecs: EC*)(using oldItsaID: Option[String])(using DbCon): HLDBTransactionResult[Unit] =
    ox.either:

      val head: EC = ecs.headOption.toRight(HLDBErrorCode.NoEntriesFound).ok()
      val idToMark = oldItsaID ?? head.getItsaID
      val hasRecord = repo.findByItsaID(idToMark).isDefined
      val errorCount = repo.getAllWithErrorSuffix(idToMark) + (if hasRecord then 1 else 0)
      val creators = ecs.filter(_.getItsaID == idToMark)
      creators.foreach: creator =>
        val currentItsaID = creator.getItsaID

        if errorCount > 0 then
          val newItsaID = s"$currentItsaID-E$errorCount "
          repo.updateItsaIDFromCreator(currentItsaID, newItsaID)
  end update

end UpdateAndInsertHelpers
private [database] object UpdateAndInsertHelpers:
  given infoUpdateAndInsertHelpers: UpdateAndInsertHelpers[InfoCreator] with
    override def update(ecs: InfoCreator*)(using oldItsaID: Option[String])(using DbCon): HLDBTransactionResult[Unit] =
      ecs.map(_.copy(lastUpdated = Timestamp.from(OffsetDateTime.now().toInstant)))
      |> super.update