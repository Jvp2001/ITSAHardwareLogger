package org.itsadigitaltrust.hardwarelogger.backend

import javax.sql.DataSource
import com.augustnagro.magnum
import com.augustnagro.magnum.{DbCodec, DbCon, DbTx, SqlException, Transactor, transact as magTransact}
import com.mysql.cj.MysqlConnection
import com.mysql.cj.jdbc.{ConnectionImpl, MysqlDataSource}
import org.itsadigitaltrust.common
import common.*
import org.itsadigitaltrust.common.Operators.??

import org.itsadigitaltrust.hardwarelogger.backend.backend.*
import org.itsadigitaltrust.hardwarelogger.backend.entities.{Wiping, WipingCreator}

import java.io.InputStream
import java.net.{URI, URL}
import java.sql.Connection
import scala.compiletime.{summonInline, uninitialized}
import scala.concurrent.Future
import scala.reflect
import scala.reflect.{ClassTag, classTag}
import scala.util.Try


class HLDatabase private(private val configFile: Try[String], private val dataSourceLoader: DataSourceLoader):

  import HLDatabase.Error
  import tables.given


  private def dataSource =
    if dataSourceLoader.dataSource.isDefined then
      dataSourceLoader.dataSource.get
    else
      dataSourceLoader(configFile)
      dataSourceLoader.dataSource.get

  private lazy val connection = dataSource.getConnection


  private given table: [EC <: HLEntityCreator : ClassTag, E <: EntityFromEC[EC]] => HLTableInfo[EC, E] = getTableInfo[EC, E]

  private given dbCodec: [EC <: ItsaEC : ClassTag] => DbCodec[EntityFromEC[EC]] = getDbCodec[EC]


  private inline def getTableInfo[EC <: ItsaEC : ClassTag, E <: EntityFromEC[EC]]: HLTableInfo[EC, E] =
    val result = classTag[EC] match
      case c if c == classTag[MemoryCreator] => tables.memoryTable
      case c if c == classTag[MediaCreator] => tables.mediaTable
      case c if c == classTag[DiskCreator] => tables.diskTable
      case c if c == classTag[InfoCreator] => tables.infoTable
      case c if c == classTag[WipingCreator] => tables.wipingTable
    result.asInstanceOf[HLTableInfo[EC, E]]


  private inline def getRepo[EC <: ItsaEC : ClassTag, E <: EntityFromEC[EC]](creator: EC): HLRepo[EC, E] =
    val result = creator.getClass match
      case c if c == classOf[MemoryCreator] => repos.memoryRepo
      case c if c == classOf[MediaCreator] => repos.mediaRepo
      case c if c == classOf[DiskCreator] => repos.diskRepo
      case c if c == classOf[InfoCreator] => repos.infoRepo
      case c if c == classOf[WipingCreator] => repos.wipingRepo
    result.asInstanceOf[HLRepo[EC, E]]
  end getRepo

  private inline def getRepo[EC <: ItsaEC : ClassTag, E <: EntityFromEC[EC]]: HLRepo[EC, E] =
    val result = summon[ClassTag[EC]] match
      case c if c == classTag[MemoryCreator] => repos.memoryRepo
      case c if c == classTag[MediaCreator] => repos.mediaRepo
      case c if c == classTag[DiskCreator] => repos.diskRepo
      case c if c == classTag[InfoCreator] => repos.infoRepo
      case c if c == classTag[WipingCreator] => repos.wipingRepo
    result.asInstanceOf[HLRepo[EC, E]]
  end getRepo


  private def getClassTagForEntityTypeEC[EC <: ItsaEC : ClassTag]: ClassTag[EntityFromEC[EC]] =
    summon[ClassTag[EntityClassTagFromEC[EC]]].asInstanceOf[ClassTag[EntityFromEC[EC]]]
  end getClassTagForEntityTypeEC

  private def getDbCodec[EC <: ItsaEC : ClassTag] =
    summon[DbCodec[EntityFromEC[EC]]]
  //    val result = summon[ClassTag[EC]] match
  //      case c if c == classTag[MemoryCreator] => summon[DbCodec[Memory]]
  //      case c if c == classTag[MediaCreator] => summon[DbCodec[Media]]
  //      case c if c == classTag[DiskCreator] => summon[DbCodec[Disk]]
  //      case c if c == classTag[InfoCreator] => summon[DbCodec[Info]]
  //      case c if c == classTag[WipingCreator] => summon[DbCodec[Wiping]]
  //      case c if c == classTag[HLEntityCreatorWithHardDiskID] => summon[DbCodec[Wiping]]
  //
  //
  //    result.asInstanceOf[DbCodec[EntityFromEC[EC]]]
  end getDbCodec


  def testConnection(): Boolean =
    dataSource.getConnection.isValid(1)
  


  def transact[T](connection: DataSource)(f: DbTx ?=> T): T =
    magTransact(connection)(f)



  /**
   * Finds the itsaID by the PC's serial number
   *
   * @param serial The serial number of the PC
   * @return [[Some]](String) if the itsaID was found, otherwise [[None]].
   */
  def findItsaIdBySerialNumber(serial: String): Option[String] =

    val ds = dataSource
    transact(ds):
      val result = repos.infoRepo.findItsaIdBySerialNumber(serial)
      result


  def insertOrUpdate[EC <: ItsaEC : ClassTag, E <: EntityFromEC[EC]](creator: EC): Unit =
    val repo = getRepo[EC, E](creator)
    transact(dataSource):
      repo.insertOrUpdate(creator)(using summon[DbCon])

  def doesDriveExists(creator: DiskCreator): Boolean =
    transact(dataSource):
      repos.diskRepo.sameDriveWithSerialNumber(creator.serial) match
        case Nil => false
        case _ => true


  def findAllByIdStartingWith[EC <: ItsaEC : ClassTag](id: String): Option[Seq[EntityFromEC[EC]]] =
    transact(dataSource):
      val repo = getRepo[EC, EntityFromEC[EC]]
      repo.findAllByIdsStartingWith(id)


  def markAllRowsWithIDAsError[EC <: ItsaEC : ClassTag](id: String): Unit =
    transact(dataSource):
      val nonErrorRows: Seq[EntityFromEC[EC]] = getRepo.findAllByID(id)
      val allRows: Seq[EntityFromEC[EC]] = getRepo.findAllByIdsStartingWith(id) ?? Seq.empty[EntityFromEC[EC]]
      val numberOfErrorRows = Math.abs(allRows.size - nonErrorRows.size)
      if numberOfErrorRows > 0 then
        val errorIndices = Range(numberOfErrorRows, allRows.size + 1)
        val newIDs = errorIndices.map: index =>
          s"${if id(id.length - 2) == '.' then id else s"$id.0"}-E$index"
        val oldToNew = nonErrorRows.map(_.id).zip(newIDs)
        oldToNew.foreach: item =>
          getRepo.replaceIDByPrimaryKey(item._1, item._2)
  end markAllRowsWithIDAsError


  def addWipingRecords(disks: WipingCreator*): Unit =
    transact(dataSource):
      repos.wipingRepo.insertAll(disks.iterator.to(Iterable))


  def findWipingRecord(serial: String): Option[Wiping] =
    given table: HLTableInfo[WipingCreator, Wiping] = tables.wipingTable

    given wipingCreatorCT: ClassTag[WipingCreator] = classTag[WipingCreator]

    val result  = transact(dataSource):
      repos.wipingRepo.findWipingRecord(serial)

    result
  end findWipingRecord

  /**
   * This method will only replace the ID in the database, not in the entity itself, if there is an entity with the old ID.
   *
   * @param old   The old ID to be replaced
   * @param `new` The new ID to replace the old one
   * @tparam EC The type of the entity creator
   */

  def replaceAllRowsWithID[EC <: ItsaEC : ClassTag](old: String, `new`: String): Unit =
    given table: HLTableInfo[EC, EntityFromEC[EC]] = getTableInfo[EC, EntityFromEC[EC]]

    transact(dataSource):
      getRepo.replaceIdWith(old, `new`)(using summon[DbCon])

  def findWipingRecordID(serial: String): Option[String] =
    findWipingRecord(serial).map(_.hddID)


  def findByID[EC <: ItsaEC : ClassTag](id: String): Option[EntityFromEC[EC]] =
    transact(dataSource):
      getRepo.findAllByID(id)(using summon[DbCon], getDbCodec).headOption

  def close(): Unit = if !connection.isClosed && connection.isValid(1) then connection.close()
end HLDatabase

object HLDatabase:
  enum Error:
    case LoaderError(error: DataSourceLoader.Error)
    case ConnectionError

  given Conversion[DataSourceLoader.Error, Error] with
    override def apply(x: DataSourceLoader.Error): Error =
      Error.LoaderError(x)

  def apply(dbProperties: => String, testConnection: Boolean = true): Result[HLDatabase, Error] =
    Result:
      val configFile = Try(dbProperties)
      DataSourceLoader(configFile) match
        case Result.Success(value) =>
          val db = new HLDatabase(configFile, value)
          if testConnection && db.testConnection() then
            db
          else
            Result.error(Error.ConnectionError)
        case org.itsadigitaltrust.common.Result.Error(_) =>
//          else
            Result.error(Error.ConnectionError)
//        case org.itsadigitaltrust.common.Result.Error(error) => Result.error(Error.LoaderError(error))



end HLDatabase





