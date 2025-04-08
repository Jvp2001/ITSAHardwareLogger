package org.itsadigitaltrust.hardwarelogger.backend

import javax.sql.DataSource
import com.augustnagro.magnum
import com.augustnagro.magnum.{BatchUpdateResult, DbCodec, DbCon, Repo, SqlException, TableInfo, Transactor, connect, sql, transact}
import com.mysql.cj.jdbc.MysqlDataSource
import org.itsadigitaltrust.common
import common.*
import org.itsadigitaltrust.hardwarelogger.backend.backend.Fragment
import org.itsadigitaltrust.hardwarelogger.backend.entities.entities.*
import org.itsadigitaltrust.hardwarelogger.backend.repos.{HLRepo, ItsaIDRepo, findAllByItsaId}
import org.itsadigitaltrust.hardwarelogger.backend.tables.HLTableInfo

import java.net.URL
import java.sql.{PreparedStatement, ResultSet, SQLException}
import scala.compiletime.{summonInline, uninitialized}
import scala.concurrent.Future

class HLDatabase private(val dataSource: DataSource):

  import HLDatabase.Error
  import tables.given

  private var connection: MysqlDataSource = uninitialized


  def getTableInfo[EC <: HLEntityCreatorWithItsaID, E <: HLEntityWithItsaID](ec: EC): HLTableInfo[EC, E] =
    val result = ec.getClass match
      case c if c == classOf[MemoryCreator] => tables.MemoryTable
      case c if c == classOf[MediaCreator] => tables.MediaTable
      case c if c == classOf[DiskCreator] => tables.DiskTable
      case c if c == classOf[InfoCreator] => tables.InfoTable
    result.asInstanceOf[HLTableInfo[EC, E]]

  def getRepo[EC <: ItsaIDRepo, E <: HLEntityWithItsaID](ec: EC): HLRepo[EC, E] =
    val result = ec.getClass match
      case c if c == classOf[MemoryCreator] => repos.MemoryRepo
      case c if c == classOf[MediaCreator] => repos.MediaRepo
      case c if c == classOf[DiskCreator] => repos.DiskRepo
      case c if c == classOf[InfoCreator] => repos.InfoRepo

    result.asInstanceOf[HLRepo[EC, E]]


  given DbCodec[Memory] = DbCodec.derived

  given DbCodec[Info] = DbCodec.derived

  given DbCodec[Disk] = DbCodec.derived

  given DbCodec[Media] = DbCodec.derived


  type EntityDbCodec[EC <: HLEntityCreatorWithItsaID] = EC match
    case MemoryCreator => DbCodec[Memory]
    case InfoCreator => DbCodec[Info]
    case DiskCreator => DbCodec[Disk]
    case MediaCreator => DbCodec[Media]

  //  type DbCodecFromTuple[T : Tuple] =
  //    T match
  //      case t *: u *: EmptyTuple => t

  def getCreatorDbCodec[EC <: HLEntityCreatorWithItsaID](creator: EC): DbCodec[MemoryCreator] | DbCodec[InfoCreator] | DbCodec[DiskCreator] | DbCodec[MediaCreator] =
    creator match
      case _: MemoryCreator => summon[DbCodec[MemoryCreator]]
      case _: InfoCreator => summon[DbCodec[InfoCreator]]
      case _: DiskCreator => summon[DbCodec[DiskCreator]]
      case _: MediaCreator => summon[DbCodec[MediaCreator]]


  def getDbCodec[EC <: HLEntityCreatorWithItsaID](creator: EC): DbCodec[HLEntity] = //DbCodec[Memory] | DbCodec[Info] | DbCodec[Disk] | DbCodec[Media] =
    val result = creator match
      case _: MemoryCreator => summon[DbCodec[Memory]]
      case _: InfoCreator => summon[DbCodec[Info]]
      case _: DiskCreator => summon[DbCodec[Disk]]
      case _: MediaCreator => summon[DbCodec[Media]]
    result.asInstanceOf[DbCodec[HLEntity]]

  def insertOrUpdate[EC <: HLEntityCreatorWithItsaID, E <: HLEntityWithItsaID](creator: EC): Unit =
    val repo = getRepo(creator)

    given table: TableInfo[EC, ?, Long] = getTableInfo(creator)

//    val transaction = Transactor(connection)


    transact(dataSource):
      repo.insertOrUpdate(creator)

  def doesDriveExists(creator: DiskCreator): Boolean =
    val transaction = Transactor(connection)
    given table: TableInfo[DiskCreator, Disk, Long] = tables.DiskTable
    transact(dataSource):
      repos.DiskRepo.sameDriveWithSerialNumber(creator.serial)(using summon[DbCon]) match
        case Nil => false
        case _ => true


object HLDatabase:
  enum Error:
    case LoaderError(error: DataStoreLoader.Error)

  given Conversion[DataStoreLoader.Error, Error] with
    override def apply(x: DataStoreLoader.Error): Error =
      Error.LoaderError(x)

  def apply(dbProperties: URL): Result[HLDatabase, DataStoreLoader.Error] =
    Result:
      DataStoreLoader(dbProperties.toURI) match
        case Success(value) =>
          Result.success(new HLDatabase(value))
        case common.Error(reason) =>
          Result.error(reason)





