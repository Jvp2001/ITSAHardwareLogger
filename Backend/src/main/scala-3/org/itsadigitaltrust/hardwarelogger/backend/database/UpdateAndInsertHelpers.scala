package org.itsadigitaltrust.hardwarelogger.backend.database

import com.augustnagro.magnum.{DbCodec, DbCon}
import org.itsadigitaltrust.common.Operators.{?, ??, |>}
import org.itsadigitaltrust.hardwarelogger.backend.{HLDBErrorCode, HLDBTransactionResult}
import org.itsadigitaltrust.hardwarelogger.backend.entities.{HLEntityCreatorWithSomeKindOfID, HLEntityWithSomeKindOfID, InfoCreator, MediaCreator, MemoryCreator}
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
      val currentItsaID = ec.getItsaID ?? HLDBErrorCode.UnknownError("No Itsa ID").fail()
      if repo.findByItsaID(currentItsaID).isEmpty then
        repo.insert(ec)
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
      Seq(idToMark, head.getItsaID).distinct.foreach: idToArchive =>
        val matchingIDs = repo.findAllByIDStartingWith(idToArchive).flatten.map(_.getItsaID)
        if matchingIDs.contains(idToArchive) then
          val nextErrorNumber = matchingIDs.iterator
            .filter(_.startsWith(s"$idToArchive-E"))
            .flatMap(_.drop(idToArchive.length + 2).toIntOption)
            .maxOption
            .getOrElse(0) + 1
          repo.updateItsaIDFromCreator(idToArchive, s"$idToArchive-E$nextErrorNumber")
  end update

end UpdateAndInsertHelpers
private [database] object UpdateAndInsertHelpers:
  given memoryUpdateAndInsertHelpers: UpdateAndInsertHelpers[MemoryCreator] with
    override def insert(ec: MemoryCreator)(using oldItsaID: Option[String])(using DbCon): HLDBTransactionResult[Unit] =
      repo.insert(ec)
      Right(())

  given mediaUpdateAndInsertHelpers: UpdateAndInsertHelpers[MediaCreator] with
    override def insert(ec: MediaCreator)(using oldItsaID: Option[String])(using DbCon): HLDBTransactionResult[Unit] =
      repo.insert(ec)
      Right(())

  given infoUpdateAndInsertHelpers: UpdateAndInsertHelpers[InfoCreator] with
    override def update(ecs: InfoCreator*)(using oldItsaID: Option[String])(using DbCon): HLDBTransactionResult[Unit] =
      ecs.map(_.copy(lastUpdated = Timestamp.from(OffsetDateTime.now().toInstant)))
      |> super.update