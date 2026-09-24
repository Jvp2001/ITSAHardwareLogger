package org.itsadigitaltrust.hardwarelogger.backend.database


import com.augustnagro.magnum
import com.augustnagro.magnum.{DbCodec, DbCon, Repo}
import com.mysql.cj.jdbc.MysqlDataSource
import com.mysql.cj.jdbc.exceptions.{MySQLQueryInterruptedException, MySQLTimeoutException}
import org.itsadigitaltrust.common.GenericTypeMapper
import org.itsadigitaltrust.common.Operators.{??, |>}
import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.hardwarelogger.backend.entities.*
import org.itsadigitaltrust.hardwarelogger.backend.{DataSourceLoader, DataSourceLoaderService, HLDBErrorCode, HLDBTransactionLabel, HLDBTransactionResult, HLSqlDataSource, repos}
import org.itsadigitaltrust.hardwarelogger.backend.repos.{HLRepoWithID, HLRepoWithSerial, infoRepo}
import ox.Ox
import ox.either.{fail, ok}

import javax.sql.DataSource
import scala.Conversion.into
import scala.compiletime.{deferred, erasedValue, summonInline}
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}
import scala.util.boundary.Label


private[database] trait RepoFromEntityCreatorGeneric[EC <: HLEntityCreatorWithSomeKindOfID, E <: HLEntityWithSomeKindOfID, R[_ <: HLEntityCreatorWithSomeKindOfID, _ <: HLEntityWithSomeKindOfID]]:
  given repo: R[EC, E] = deferred

type RepoFromEntityCreator[EC <: HLEntityCreatorWithSomeKindOfID, E <: HLEntityWithSomeKindOfID] = RepoFromEntityCreatorGeneric[EC, E, HLRepoWithID]


/**
 * Only works on multiple models at once, as most tables have multiple rows for each PC.
 * @tparam EC The [[HLEntityCreatorWithSomeKindOfID]] that this instance requires.
 */
trait UpdateAndInsert[EC <: HLEntityCreatorWithSomeKindOfID]:


  def updateAndInsertAll(using oldItsaID: Option[String] = None)(entityCreators: Seq[EC])(using DbCon): HLDBTransactionResult[Unit]



trait HLDatabase(using dataSourceLoader: DataSourceLoaderService[HLSqlDataSource]) extends HWLLoggable:


  def connect()(using ox.Ox): Either[HLDBErrorCode, Unit]


  def insert[EC <: HLEntityCreatorWithSomeKindOfID](entityCreator: EC)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Unit]


  def insertAll[EC <: HLEntityCreatorWithSomeKindOfID](entities: Seq[EC])(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Unit]

  def findAllStartingWithItsaID[EC <: HLEntityCreatorWithSomeKindOfID](itsaID: String)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Vector[mapper.Out]]

  def findAllByItsaID[EC <: HLEntityCreatorWithSomeKindOfID](itsaID: String)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Vector[mapper.Out]]

  def findFirstByItsaID[EC <: HLEntityCreatorWithSomeKindOfID](itsaID: String)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[mapper.Out]

  def findItsaIDBySerialNumber[EC <: HLEntityCreatorWithSomeKindOfID](serialNumber: String)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[String]

  def findBySerialNumber[EC <: HLEntityCreatorWithSomeKindOfID](serialNumber: String)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[mapper.Out]

  def findAllByItsaIDEndingWith[EC <: HLEntityCreatorWithSomeKindOfID](suffix: String)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Vector[mapper.Out]]



  def updateAndInsertAll[EC <: HLEntityCreatorWithSomeKindOfID : UpdateAndInsert as handler](entityCreators: EC*)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Unit]

  def deleteByItsaID[EC <: HLEntityCreatorWithSomeKindOfID](itsaID: String)(using mapper: EntityCreatorToEntityMapper[EC], repoFromEntityCreator: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Unit]

  def deleteByAllSerials[EC <: HLEntityCreatorWithSomeKindOfID](serials: String*)(using mapper: EntityCreatorToEntityMapper[EC], repoFromEntityCreator: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Unit]




end HLDatabase

object HLDatabase:


  given updateAndInsertHelpers[EC <: HLEntityCreatorWithSomeKindOfID : ClassTag](using ecMapper: EntityCreatorToEntityMapper[EC], repoWithItsaID: HLRepoWithID[EC, ecMapper.Out]): UpdateAndInsertHelpers[EC] with
    override def insert(ec: EC)(using oldItsaID: Option[String])(using DbCon): HLDBTransactionResult[Unit] =
      super.insert(ec)

  given updateAndInsert[EC <: HLEntityCreatorWithSomeKindOfID : {ClassTag}, E <: HLEntityWithSomeKindOfID](using repoWithItsaID: HLRepoWithID[EC, E], helpers: UpdateAndInsertHelpers[EC]): UpdateAndInsert[EC] with

    override def updateAndInsertAll(using oldItsaID: Option[String])(entityCreators: Seq[EC])(using DbCon): HLDBTransactionResult[Unit] =
      ox.either:
        helpers.update(entityCreators*).left.map(_.fail())
        entityCreators.map(helpers.insert).map(_.left.map(_.fail()))


end HLDatabase

class MySQLHLDatabase(using dataSourceLoader: DataSourceLoaderService[HLSqlDataSource]) extends HLDatabase(using dataSourceLoader):

  val dataSource: Either[HLDBErrorCode, HLSqlDataSource] = dataSourceLoader.dataSource
  override def connect()(using Ox): Either[HLDBErrorCode, Unit] =
    ox.either:
      try
        val ds: MysqlDataSource = dataSource.ok()
        magnum.connect(ds):
          ()
      catch
        case e: MySQLTimeoutException =>
          HLDBErrorCode.ConnectionError("Unknown", e.getMessage).fail()
        case e: MySQLQueryInterruptedException =>
          HLDBErrorCode.ConnectionError("Unknown", e.getMessage).fail()
  end connect


  override def findBySerialNumber[EC <: HLEntityCreatorWithSomeKindOfID](serialNumber: String)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[mapper.Out] =
    transact:
      itsaIDRepo match
        case serial: HLRepoWithSerial[EC, mapper.Out] =>
          val result = serial.findBySerialNumber(serialNumber)
          if result.isDefined then
            Right(result.get)
          else
            HLDBErrorCode.NoEntriesFound.fail()
        case _ => HLDBErrorCode.UnknownError("Must be a serial repo to find by serial number!").fail()

  end findBySerialNumber


  override def findAllByItsaIDEndingWith[EC <: HLEntityCreatorWithSomeKindOfID](suffix: String)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Vector[mapper.Out]] =
    transact:
      itsaIDRepo.findAllByIDEndingWith(suffix).filter(_.isDefined).map(_.get) match
        case result if result.isEmpty =>
          HLDBErrorCode.NoEntriesFound.fail()
        case result =>
          Right(result)
  end findAllByItsaIDEndingWith

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
        val executionAttempt = Try(body)
        executionAttempt match
          case Failure(exception) => HLDBErrorCode.UnknownError(exception.getMessage).fail()
          case Success(value) => value.ok()

  override def findItsaIDBySerialNumber[EC <: HLEntityCreatorWithSomeKindOfID](serialNumber: String)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[String] =
    transact[String]:
      repo match
        case serialRepo: HLRepoWithSerial[EC, mapper.Out] =>
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
      val result = itsaIDRepo.findAllByIDStartingWith(itsaID)
      if result.isEmpty then
        HLDBErrorCode.NoEntriesFound.fail()
      Right(result.filter(_.isDefined).map(_.get))
  end findAllByItsaID

  override def findFirstByItsaID[EC <: HLEntityCreatorWithSomeKindOfID](itsaID: String)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[mapper.Out] =
    transact:
      itsaIDRepo.findByItsaID(itsaID).toRight(HLDBErrorCode.NoEntriesFound)
  
  override def updateAndInsertAll[EC <: HLEntityCreatorWithSomeKindOfID : UpdateAndInsert as handler](entityCreators: EC*)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out]): HLDBTransactionResult[Unit] =
    transact:
      handler.updateAndInsertAll(entityCreators)

  private def insertHelper[EC <: HLEntityCreatorWithSomeKindOfID](entityCreator: EC)(using mapper: EntityCreatorToEntityMapper[EC], repo: RepoFromEntityCreator[EC, mapper.Out], dbCon: DbCon): HLDBTransactionResult[Unit] =
    ox.either:
      if itsaIDRepo.findByItsaID(entityCreator.getItsaID).isDefined then
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





  private transparent inline given itsaIDRepo[EC <: HLEntityCreatorWithSomeKindOfID](using mapper: EntityCreatorToEntityMapper[EC], fromRepo: RepoFromEntityCreator[EC, mapper.Out]): HLRepoWithID[EC, mapper.Out] =
    fromRepo.repo


end MySQLHLDatabase

