package org.itsadigitaltrust.hardwarelogger.backend

import javax.sql.DataSource
import com.augustnagro.magnum.*
import com.mysql.cj.jdbc.MysqlDataSource
import org.itsadigitaltrust.common
import common.*
import org.itsadigitaltrust.hardwarelogger.backend.backend.*
import org.itsadigitaltrust.hardwarelogger.backend.entities.{Wiping, WipingCreator}

import java.net.URL
import java.sql.{PreparedStatement, ResultSet, SQLException}
import scala.compiletime.{summonInline, uninitialized}
import scala.concurrent.Future
import scala.reflect
import scala.reflect.{ClassTag, classTag}


class HLDatabase private(dataSource: DataSource):

  import HLDatabase.Error
  import tables.given

  private val connection: DataSource = dataSource

  private given table: [EC <: HLEntityCreator : ClassTag, E <: EntityFromEC[EC]] => HLTableInfo[EC, E] = getTableInfo[EC, E]
  private given dbCodec: [EC <: ItsaEC : ClassTag] => DbCodec[EntityFromEC[EC]] =
    getDbCodec[EC]

  def getTableInfo[EC <: ItsaEC : ClassTag, E <: EntityFromEC[EC]]: HLTableInfo[EC, E] =
    val result = classTag[EC] match
      case c if c == classTag[MemoryCreator] => tables.memoryTable
      case c if c == classTag[MediaCreator] => tables.mediaTable
      case c if c == classTag[DiskCreator] => tables.diskTable
      case c if c == classTag[InfoCreator] => tables.infoTable
      case c if c == classTag[WipingCreator] => tables.wipingTable
    result.asInstanceOf[HLTableInfo[EC, E]]


  def getRepo[EC <: ItsaEC : ClassTag, E <: EntityFromEC[EC]](creator: EC): HLRepo[EC, E] =
    val result = creator.getClass match
      case c if c == classOf[MemoryCreator] => repos.memoryRepo
      case c if c == classOf[MediaCreator] => repos.mediaRepo
      case c if c == classOf[DiskCreator] => repos.diskRepo
      case c if c == classOf[InfoCreator] => repos.infoRepo
      case c if c == classOf[WipingCreator] => repos.wipingRepo
    result.asInstanceOf[HLRepo[EC, E]]
  end getRepo

  def getRepo[EC <: ItsaEC : ClassTag, E <: EntityFromEC[EC]]: HLRepo[EC, E] =
    val result = summon[ClassTag[EC]]match
      case c if c == classTag[MemoryCreator] => repos.memoryRepo
      case c if c == classTag[MediaCreator] => repos.mediaRepo
      case c if c == classTag[DiskCreator] => repos.diskRepo
      case c if c == classTag[InfoCreator] => repos.infoRepo
      case c if c == classTag[WipingCreator] => repos.wipingRepo
    result.asInstanceOf[HLRepo[EC, E]]
  end getRepo




  private def getClassTagForEntityTypeEC[EC <: ItsaEC : ClassTag]: ClassTag[EntityFromEC[EC]] =
    val result = summon[ClassTag[EC]].getClass match
      case c if c == classOf[Memory] => summon[ClassTag[Memory]]
      case c if c == classOf[Media] => summon[ClassTag[Media]]
      case c if c == classOf[Disk] => summon[ClassTag[Disk]]
      case c if c == classOf[Info] => summon[ClassTag[Info]]
      case c if c == classOf[Wiping] => summon[ClassTag[Wiping]]
    result.asInstanceOf[ClassTag[EntityFromEC[EC]]]
  end getClassTagForEntityTypeEC

  private def getDbCodec[EC <: ItsaEC : ClassTag] =
    val result = summon[ClassTag[EC]] match
      case c if c == classTag[MemoryCreator] => summon[DbCodec[Memory]]
      case c if c == classTag[MediaCreator] => summon[DbCodec[Media]]
      case c if c == classTag[DiskCreator] => summon[DbCodec[Disk]]
      case c if c == classTag[InfoCreator] => summon[DbCodec[Info]]
      case c if c == classTag[WipingCreator] => summon[DbCodec[Wiping]]
      case c  if c == classTag[HLEntityCreatorWithHardDiskID] => summon[DbCodec[Wiping]]

    result.asInstanceOf[DbCodec[EntityFromEC[EC]]]
  end getDbCodec



  def findItsaIdBySerialNumber(serial: String): Option[String] =
    val transaction = Transactor(connection)
    transact(transaction):
      val result = repos.infoRepo.findItsaIdBySerialNumber(serial)
      result

  def insertOrUpdate[EC <: ItsaEC : ClassTag, E <: EntityFromEC[EC]](creator: EC): Unit =
    val repo = getRepo[EC, E](creator)
    val transaction = Transactor(connection)
    transact(transaction):
      repo.insertOrUpdate(creator)(using summon[DbCon])

  def doesDriveExists(creator: DiskCreator): Boolean =
    val transaction = Transactor(connection)
    transact(transaction):
      repos.diskRepo.sameDriveWithSerialNumber(creator.serial) match
        case Nil => false
        case _ => true


  def findAllByIdStartingWith[EC <: ItsaEC : ClassTag ](id: String): Seq[EntityFromEC[EC]] =
    val transaction = Transactor(connection)
    transact(transaction):
      getRepo[EC, EntityFromEC[EC]].findAllByIdsStartingWith(id)



  def markAllRowsWithIDAsError[EC <: ItsaEC : ClassTag](id: String): Unit =
    val transaction = Transactor(connection)

    transact(transaction):
      val nonErrorRows:Seq[EntityFromEC[EC]] = getRepo.findAllByID(id)
      val allRows: Seq[EntityFromEC[EC]] = getRepo.findAllByIdsStartingWith(id)
      val numberOfErrorRows = Math.abs(allRows.size - nonErrorRows.size)
      if numberOfErrorRows > 0 then
        val errorIndices = Range(numberOfErrorRows, allRows.size+1)
        val newIDs = errorIndices.map: index =>
          s"${if id(id.length-2) == '.' then id else s"$id.0" }-E$index"
        val oldToNew =  nonErrorRows.map(_.id).zip(newIDs)
        oldToNew.foreach: item =>
          getRepo.replaceIDByPrimaryKey(item._1, item._2)
  end markAllRowsWithIDAsError


  def addWipingRecords(disks: WipingCreator*): Unit =
    transact(Transactor(connection)):
      repos.wipingRepo.insertAll(disks.iterator.to(Iterable))


  def findWipingRecord(serial: String) : Option[Wiping] =
    val transaction = Transactor(connection)
    given table: HLTableInfo[WipingCreator, Wiping] = tables.wipingTable
    given wipingCreatorCT: ClassTag[WipingCreator] = classTag[WipingCreator]
    transact(transaction):
      repos.wipingRepo.findWipingRecord(serial)(using summon[DbCon])(using table)
  end findWipingRecord

  /**
   * This method will only replace the ID in the database, not in the entity itself, if there is an entity with the old ID.
   *
   * @param old   The old ID to be replaced
   * @param `new` The new ID to replace the old one
   * @tparam EC The type of the entity creator
   */

  def replaceAllRowsWithID[EC <: ItsaEC : ClassTag](old: String, `new`: String): Unit =
    val transaction = Transactor(connection)
    given table: HLTableInfo[EC, EntityFromEC[EC]] = getTableInfo[EC, EntityFromEC[EC]]
    transact(transaction):
      getRepo.replaceIdWith(old, `new`)(using summon[DbCon])

  def findByID[EC <: ItsaEC : ClassTag](id: String): Option[EntityFromEC[EC]] =
    val transaction = Transactor(connection)
    transact(transaction):
      getRepo.findAllByID(id)(using summon[DbCon], getDbCodec).headOption

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





