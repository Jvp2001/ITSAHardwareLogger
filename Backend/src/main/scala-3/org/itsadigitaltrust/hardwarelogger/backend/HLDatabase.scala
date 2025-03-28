package org.itsadigitaltrust.hardwarelogger.backend

import javax.sql.DataSource
import com.augustnagro.magnum
import com.augustnagro.magnum.{BatchUpdateResult, DbCodec, Repo, SqlException, TableInfo, Transactor, connect, sql, transact}
import com.mysql.cj.jdbc.MysqlDataSource
import org.itsadigitaltrust.common
import org.itsadigitaltrust.common.{Result, optional}
import org.itsadigitaltrust.hardwarelogger.backend.backend.Fragment
import org.itsadigitaltrust.hardwarelogger.backend.entities.{Disk, DiskCreator, HLEntity, HLEntityCreator, InfoCreator, MediaCreator, Memory, MemoryCreator}
import org.itsadigitaltrust.hardwarelogger.backend.repos.HLRepo
import org.itsadigitaltrust.hardwarelogger.backend.tables.HLTableInfo

import java.net.URL
import java.sql.SQLException
import scala.compiletime.uninitialized
import scala.concurrent.Future
import scala.util.{Failure, Success, Try, boundary}

class HLDatabase private(val dataSource: DataSource):

  import HLDatabase.Error
  import tables.given

  private var connection: MysqlDataSource = uninitialized


  def getTableInfo[EC <: HLEntityCreator, E <: HLEntity](ec: EC): HLTableInfo[EC, E] =
    val result = ec.getClass match
      case c if c == classOf[MemoryCreator] => tables.MemoryTable
      case c if c == classOf[MediaCreator] => tables.MediaTable
      case c if c == classOf[DiskCreator] => tables.DiskTable
      case c if c == classOf[InfoCreator] => tables.InfoTable
    result.asInstanceOf[HLTableInfo[EC, E]]

  def getRepo[EC <: HLEntityCreator, E <: HLEntity](ec: EC): HLRepo[EC, E] =
    val result = ec.getClass match
      case c if c == classOf[MemoryCreator] => repos.MemoryRepo
      case c if c == classOf[MediaCreator] => repos.MediaRepo
      case c if c == classOf[DiskCreator] => repos.DiskRepo
      case c if c == classOf[InfoCreator] => repos.InfoRepo

    result.asInstanceOf[HLRepo[EC, E]]

  
  

  def insertOrUpdate[EC <: HLEntityCreator](creator: EC): Unit =
    val repo = getRepo(creator)
    val table: TableInfo[EC, ? <: HLEntity, Long] = getTableInfo(creator)
    val transaction = Transactor(connection)
    connect(dataSource):
      repo.insert(creator)


//  def insertOrUpdate[EC <: HLEntityCreator, E <: HLEntity](creator: EC)(using repo: HLRepo[EC, E])(using TableInfo[EC, E, Long]): Unit =
//    magnum.transact(connection):
//      repo.insertOrUpdate(creator)
//
//  def insertOrUpdate(creator: DiskCreator): Unit =
//    insertOrUpdate[DiskCreator, Disk](creator)
//
//  def insertOrUpdate(creator: MemoryCreator): Unit =
//    insertOrUpdate[MemoryCreator, Memory](creator)



  private def error(e: Error)(using label: boundary.Label[Error]): Nothing =
    boundary.break(e)

object HLDatabase:
  enum Error:
    case LoaderError(error: DataStoreLoader.Error)

  given Conversion[DataStoreLoader.Error, Error] with
    override def apply(x: DataStoreLoader.Error): Error =
      Error.LoaderError(x)

  def apply(dbProperties: URL): Result[HLDatabase, DataStoreLoader.Error] =
    Result:
      DataStoreLoader(dbProperties.toURI) match
        case common.Success(value) =>
          Result.success(new HLDatabase(value))
        case common.Error(reason) =>
          Result.error(reason)





