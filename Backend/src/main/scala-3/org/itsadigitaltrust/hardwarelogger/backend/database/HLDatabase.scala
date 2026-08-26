package org.itsadigitaltrust.hardwarelogger.backend.database


import com.augustnagro.magnum
import com.augustnagro.magnum.{DbCodec, DbCon, Repo}
import com.mysql.cj.jdbc.MysqlDataSource
import org.itsadigitaltrust.common.GenericTypeMapper
import org.itsadigitaltrust.common.Operators.??
import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.hardwarelogger.backend.database.HLDatabase.diskRepoFromEC
import org.itsadigitaltrust.hardwarelogger.backend.{DataSourceLoader, DataSourceLoaderService, HLDBErrorCode, HLDBTransactionLabel, HLDBTransactionResult, HLSqlDataSource, repos}
import org.itsadigitaltrust.hardwarelogger.backend.entities.{Disk, DiskCreator, HLEntityCreatorWithSomeKindOfID, HLEntityWithSomeKindOfID, Info, InfoCreator, Media, MediaCreator, Memory, MemoryCreator, Wiping, WipingCreator}
import org.itsadigitaltrust.hardwarelogger.backend.repos.{HLRepoWithID, HLRepoWithSerial, infoRepo}
import ox.either.{fail, ok}
import sun.jvm.hotspot.runtime.PerfMemory.end

import javax.sql.DataSource
import scala.Conversion.into
import scala.compiletime.{deferred, erasedValue, summonInline}
import scala.util.{Failure, Success, Try}
import scala.util.boundary.Label


private[database] trait RepoFromEntityCreatorGeneric[EC <: HLEntityCreatorWithSomeKindOfID, E <: HLEntityWithSomeKindOfID, R[_ <: HLEntityCreatorWithSomeKindOfID, _ <: HLEntityWithSomeKindOfID]]:
  given repo: R[EC, E] = deferred

type RepoFromEntityCreator[EC <: HLEntityCreatorWithSomeKindOfID, E <: HLEntityWithSomeKindOfID] = RepoFromEntityCreatorGeneric[EC, E, HLRepoWithID]


trait UpdateOrInsert[EC <: HLEntityCreatorWithSomeKindOfID]:
  def updateOrInsert(entityCreator: EC)(using DbCon): HLDBTransactionResult[Unit]
  def updateOrInsertAll(entityCreators: Seq[EC])(using DbCon): HLDBTransactionResult[Unit] =
    ox.either:
      entityCreators.map: entityCreator =>
        updateOrInsert(entityCreator).ok()

trait EntityCreatorToEntityMapper[EC <: HLEntityCreatorWithSomeKindOfID : DbCodec] extends GenericTypeMapper[EC, HLEntityWithSomeKindOfID]:
  override type Out <: HLEntityWithSomeKindOfID : DbCodec


private[database] trait TypeClassGivens:
  self: HLDatabase & HWLLoggable =>


  given updateOrInsertInfo: UpdateOrInsert[InfoCreator] with
    override def updateOrInsert(infoCreator: InfoCreator)(using DbCon): HLDBTransactionResult[Unit] =
      ox.either:
        val repo = repos.infoRepo
        val result = repo.findAllWithErrorNumber(infoCreator.itsaID)
        val errorCount = repo.getAllWithErrorSuffix(infoCreator.itsaID)
        val newErrorCountValue = errorCount + 1
        val findLastValid = repo.findFirstByItsaID(infoCreator.itsaID) ?? HLDBErrorCode.NoEntriesFound.fail()

        repo.update(findLastValid.copy(itsaID = s"${infoCreator.itsaID}-E$newErrorCountValue"))
        repo.insert(infoCreator)
        Right(Vector())
    end updateOrInsert


end TypeClassGivens

trait HLDatabase(using dataSourceLoader: DataSourceLoaderService[HLSqlDataSource]) extends TypeClassGivens, HWLLoggable:

  def insert[EC <: HLEntityCreatorWithSomeKindOfID](entityCreator: EC)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Unit]


  def insertAll[EC <: HLEntityCreatorWithSomeKindOfID](entities: Seq[EC])(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Unit]

  def findAllStartingWithItsaID[EC <: HLEntityCreatorWithSomeKindOfID](itsaID: String)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Vector[mapper.Out]]

  def findAllByItsaID[EC <: HLEntityCreatorWithSomeKindOfID](itsaID: String)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Vector[mapper.Out]]

  def findFirstByItsaID[EC <: HLEntityCreatorWithSomeKindOfID](itsaID: String)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[mapper.Out]

  def findItsaIDBySerialNumber[EC <: HLEntityCreatorWithSomeKindOfID](serialNumber: String)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[String]

  def findBySerialNumber[EC <: HLEntityCreatorWithSomeKindOfID](serialNumber: String)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[mapper.Out]

  def updateOrInsert[EC <: HLEntityCreatorWithSomeKindOfID : UpdateOrInsert as handler](entityCreator: EC)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Unit]

  def updateOrInsertAll[EC <: HLEntityCreatorWithSomeKindOfID : UpdateOrInsert as handler](entityCreators: Seq[EC])(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Unit]

  def deleteByItsaID[EC <: HLEntityCreatorWithSomeKindOfID](itsaID: String)(using mapper: EntityCreatorToEntityMapper[EC], repoFromEntityCreator: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Unit]

  def deleteByAllSerials[EC <: HLEntityCreatorWithSomeKindOfID](serials: String*)(using mapper: EntityCreatorToEntityMapper[EC], repoFromEntityCreator: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Unit]

  def replaceItsaID[EC <: HLEntityCreatorWithSomeKindOfID](oldItsaID: String, newItsaID: String)(using mapper: EntityCreatorToEntityMapper[EC], repoFromEntityCreator: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Unit]


end HLDatabase

object HLDatabase:

  given infoRepoFromEC: RepoFromEntityCreator[InfoCreator, Info] with
    override given repo: HLRepoWithID[InfoCreator, Info] = repos.infoRepo

  given memoryRepoFromEC: RepoFromEntityCreator[MemoryCreator, Memory] with
    override given repo: HLRepoWithID[MemoryCreator, Memory] = repos.memoryRepo

  given diskRepoFromEC: RepoFromEntityCreator[DiskCreator, Disk] with
    override given repo: HLRepoWithID[DiskCreator, Disk] = repos.diskRepo


  given mediaRepoFromEC: RepoFromEntityCreator[MediaCreator, Media] with
    override given repo: HLRepoWithID[MediaCreator, Media] = repos.mediaRepo

  given wipingRepoFromEC: RepoFromEntityCreator[WipingCreator, Wiping] with
    override given repo: HLRepoWithID[WipingCreator, Wiping] = repos.wipingRepo

  given infoECToInfo: EntityCreatorToEntityMapper[InfoCreator] with
    override type Out = Info

  given memoryECToMemory: EntityCreatorToEntityMapper[MemoryCreator] with
    override type Out = Memory

  given diskECToDisk: EntityCreatorToEntityMapper[DiskCreator] with
    override type Out = Disk

  given mediaECToMedia: EntityCreatorToEntityMapper[MediaCreator] with
    override type Out = Media

  given wipingECToWiping: EntityCreatorToEntityMapper[WipingCreator] with
    override type Out = Wiping

end HLDatabase

class MySQLHLDatabase(using dataSourceLoader: DataSourceLoaderService[HLSqlDataSource]) extends HLDatabase(using dataSourceLoader):


  override def findBySerialNumber[EC <: HLEntityCreatorWithSomeKindOfID](serialNumber: String)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[mapper.Out] =
    transact:
      itsaIDRepo match
        case serial: HLRepoWithSerial[EC, mapper.Out] =>
          val result = serial.findFirstBySerial(serialNumber)
          if result.isDefined then
            Right(result.get)
          else
            HLDBErrorCode.NoEntriesFound.fail()
        case _ => HLDBErrorCode.UnknownError("Must be a serial repo to find by serial number!").fail()

  end findBySerialNumber

  override def findAllStartingWithItsaID[EC <: HLEntityCreatorWithSomeKindOfID](itsaID: String)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Vector[mapper.Out]] =
    transact:
      val result = itsaIDRepo.findAllByIDStartingWith(itsaID).filter(_.isDefined).map(_.get)
      if result.isEmpty then
        HLDBErrorCode.NoEntriesFound.fail()
      Right(result)

  private def transact[R](body: magnum.DbTx ?=> HLDBTransactionLabel[R] ?=> HLDBTransactionResult[R]): HLDBTransactionResult[R] =

    ox.either:
      val dataSource = dataSourceLoader.dataSource.ok()
      magnum.transact(dataSource):
        Try(body) match
          case Failure(exception) => HLDBErrorCode.UnknownError(exception.getMessage).fail()
          case Success(value) => value.ok()
  end transact

  override def findItsaIDBySerialNumber[EC <: HLEntityCreatorWithSomeKindOfID](serialNumber: String)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[String] =
    transact[String]:
      repo match
        case serialRepo: HLRepoWithSerial[EC, ?] =>
          val result = serialRepo.findItsaIDBySerialNumber(serialNumber)
          if result.isDefined then
            Right(result.get)
          else
            Left(HLDBErrorCode.NoEntriesFound.fail())
        case _ => Left(HLDBErrorCode.UnknownError("Must be a serial repo to find itsaID by serial number!"))

  override def insert[EC <: HLEntityCreatorWithSomeKindOfID](entityCreator: EC)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Unit] =
    transact:
      itsaIDRepo.insert(entityCreator)
      Right(())

  override def insertAll[EC <: HLEntityCreatorWithSomeKindOfID](entities: Seq[EC])(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Unit] =
    transact:
      itsaIDRepo.insertAll(entities)
      Right(())

  override def findAllByItsaID[EC <: HLEntityCreatorWithSomeKindOfID](itsaID: String)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Vector[mapper.Out]] =
    transact:
      val result = itsaIDRepo.findAllByItsaID(itsaID)
      if result.isEmpty then
        HLDBErrorCode.NoEntriesFound.fail()
      Right(result)
  end findAllByItsaID

  override def findFirstByItsaID[EC <: HLEntityCreatorWithSomeKindOfID](itsaID: String)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[mapper.Out] =
    transact:
      itsaIDRepo.findFirstByItsaID(itsaID).toRight(HLDBErrorCode.NoEntriesFound)


  override def updateOrInsert[EC <: HLEntityCreatorWithSomeKindOfID : UpdateOrInsert as handler](entityCreator: EC)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Unit] =
    transact:
      handler.updateOrInsert(entityCreator)


  override def updateOrInsertAll[EC <: HLEntityCreatorWithSomeKindOfID : UpdateOrInsert as handler](entityCreators: Seq[EC])(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Unit] =
    transact:
      Right(entityCreators.foreach(handler.updateOrInsert))
  
  private def insertHelper[EC <: HLEntityCreatorWithSomeKindOfID](entityCreator: EC)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out], dbCon: DbCon): HLDBTransactionResult[Unit] =
    ox.either:
      if itsaIDRepo.findFirstByItsaID(entityCreator.getItsaID).isDefined then
        itsaIDRepo.markRowAsError(entityCreator.getItsaID)

      itsaIDRepo match
        case serialRepo: HLRepoWithSerial[EC, ?] =>
          serialRepo.updateItsaIDBySerial(entityCreator)
        case _ => ()
  end insertHelper



  override def deleteByItsaID[EC <: HLEntityCreatorWithSomeKindOfID](itsaID: String)(using mapper: EntityCreatorToEntityMapper[EC], repoFromEntityCreator: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Unit] =
    transact:
       itsaIDRepo.deleteByItsaID(itsaID) match
          case 0 => HLDBErrorCode.NoEntriesFound.fail()
          case _ => Right(())
  end deleteByItsaID

  override def deleteByAllSerials[EC <: HLEntityCreatorWithSomeKindOfID](serials: String*)(using mapper: EntityCreatorToEntityMapper[EC], repoFromEntityCreator: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Unit] =
    transact:
      infoRepo match
        case serialRepo: HLRepoWithSerial[EC, ?] =>
          val result = serials.map: serial =>
            serialRepo.deleteBySerial(serial)
          .sum
          if result == 0 then
            HLDBErrorCode.NoEntriesRemoved.fail()
          else
            Right(())
  end deleteByAllSerials


  override def replaceItsaID[EC <: HLEntityCreatorWithSomeKindOfID](oldItsaID: String, newItsaID: String)(using mapper: EntityCreatorToEntityMapper[EC], repoFromEntityCreator: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Unit] =
    transact:
      itsaIDRepo.replaceItsaID(oldItsaID, newItsaID)
      Right(())
  



  private transparent inline given itsaIDRepo[EC <: HLEntityCreatorWithSomeKindOfID](using mapper: EntityCreatorToEntityMapper[EC], fromRepo: RepoFromEntityCreator[EC, mapper.Out]): HLRepoWithID[EC, mapper.Out] =
    fromRepo.repo


end MySQLHLDatabase

