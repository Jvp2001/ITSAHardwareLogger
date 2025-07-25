package org.itsadigitaltrust.hardwarelogger.backend

import org.itsadigitaltrust.common.*
import org.itsadigitaltrust.common.Operators.??
import org.itsadigitaltrust.common.logging.HWLLoggable

import org.itsadigitaltrust.hardwarelogger.backend
import org.itsadigitaltrust.hardwarelogger.backend.backend.*
import org.itsadigitaltrust.hardwarelogger.backend.entities.ItsaIDOfSomeKind

import com.augustnagro.magnum.{DbCodec, DbCon, DbTx, transact as magTransact}
import org.itsadigitaltrust.common

import javax.sql.DataSource
import scala.collection.mutable
import scala.compiletime.summonInline
import scala.reflect.{ClassTag, classTag}
import scala.util.Try

class HLDatabase private(private val configFile: Try[String], private val dataSourceLoader: DataSourceLoader) extends HWLLoggable:

  import tables.given


  private def dataSource =
    if dataSourceLoader.dataSource.isDefined then
      dataSourceLoader.dataSource.get
    else
      dataSourceLoader(configFile)
      dataSourceLoader.dataSource.get

  private lazy val connection = new net.sf.log4jdbc.ConnectionSpy(dataSource.getConnection)
  private lazy val letters = 'A' to 'Z'
  private final val lettersInTheAlphabet = 26

  private given table: [EC <: HLEntityCreator : ClassTag, E <: EntityFromEC[EC]] => HLTableInfo[EC, E] =
    logger.info(s"Table Info: ${getTableInfo[EC, E]}")
    getTableInfo[EC, E]

  private given dbCodec: [EC <: ItsaEC : ClassTag] => DbCodecFromEC[EC] = getDbCodec[EC]


  private inline def getTableInfo[EC <: ItsaEC : ClassTag, E <: EntityFromEC[EC]]: HLTableInfo[EC, E] =
    logger.info(s"Class Tag: ${summonInline[ClassTag[EC]]}")
    val result = summonInline[ClassTag[EC]] match
      case c if c == classTag[MemoryCreator] =>
        tables.memoryTable
      case c if c == classTag[MediaCreator] =>
        tables.mediaTable
      case c if c == classTag[DiskCreator] =>
        tables.diskTable
      case c if c == classTag[InfoCreator] =>
        tables.infoTable
      case c if c == classTag[WipingCreator] =>
        tables.wipingTable
      case _ =>
        scala.sys.error(s"Unknown entity creator class: ${summonInline[ClassTag[EC]]}")
        throw new IllegalArgumentException(s"Unknown entity creator class: ${summonInline[ClassTag[EC]]}")
    result.asInstanceOf[HLTableInfo[EC, E]]


  private inline def getRepo[EC <: ItsaEC, E <: EntityFromEC[EC]](creator: EC): HLRepo[EC, E] =
    val result = creator.getClass match
      case c if c == classOf[MemoryCreator] =>
        logger.info("Memory Repo")
        repos.memoryRepo
      case c if c == classOf[MediaCreator] =>
        logger.info("Media Repo")
        repos.mediaRepo
      case c if c == classOf[DiskCreator] =>
        logger.info("Disk Repo")
        repos.diskRepo
      case c if c == classOf[InfoCreator] =>
        logger.info("Info repo")
        repos.infoRepo
      case c if c == classOf[WipingCreator] =>
        logger.info("Wiping repo")
        repos.wipingRepo
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
    val result = summon[ClassTag[EC]] match
      case c if c == classTag[MemoryCreator] => summon[DbCodec[Memory]]
      case c if c == classTag[MediaCreator] => summon[DbCodec[Media]]
      case c if c == classTag[DiskCreator] => summon[DbCodec[Disk]]
      case c if c == classTag[InfoCreator] => summon[DbCodec[Info]]
      case c if c == classTag[WipingCreator] => summon[DbCodec[Wiping]]
      case c if c == classTag[HLEntityCreatorWithHardDiskID] => summon[DbCodec[Wiping]]


    result.asInstanceOf[DbCodec[EntityFromEC[EC]]]
  end getDbCodec


  def testConnection(): Boolean =
    dataSource.getConnection.isValid(1)


  def transact[T](connection: DataSource)(f: DbTx ?=> T): Try[T] =
    Try(magTransact(connection)(f))


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
    .unwrapSafe


  def insertOrUpdate[EC <: ItsaEC : ClassTag, E <: EntityFromEC[EC]](creator: EC): Unit =
    val repo = getRepo[EC, E](creator)
    logger.info(s"Repo: $repo")
    given table:HLTableInfo[EC, E] = repo match
      case repos.wipingRepo =>
        tables.wipingTable
      case repos.mediaRepo =>
        tables.mediaTable
      case repos.diskRepo =>
        tables.diskTable
      case repos.infoRepo =>
        tables.infoTable
      case repos.memoryRepo =>
        tables.memoryTable
    transact(dataSource):
      repo.insertOrUpdate(creator)



  def doesDriveExists(creator: DiskCreator): Boolean =
    transact(dataSource):
      repos.diskRepo.sameDriveWithSerialNumber(creator.serial) match
        case Nil => false
        case _ => true
    .toOption ?? false


  def findAllByIdStartingWith[EC <: ItsaEC : ClassTag](id: String): Option[Seq[EntityFromEC[EC]]] =
    transact(dataSource):
      val repo = getRepo[EC, EntityFromEC[EC]]
      repo.findAllByIDStartingWith(id)
    .toOptionFlat


  def markAllRowsWithIDAsError[EC <: ItsaEC : ClassTag](id: String): Unit =
    transact(dataSource):
      given tableInfo: HLTableInfo[EC, EntityFromEC[EC]] = getTableInfo
      logger.info(s"Marking rows with id: $id as error!")
      if id == null || id.isEmpty then
        ()
      else
        val nonErrorRows: Seq[EntityFromEC[EC]] = getRepo.findAllByID(id)
        val allRows: Seq[EntityFromEC[EC]] = getRepo.findAllByIDStartingWith(id) ?? Seq.empty[EntityFromEC[EC]]
        val numberOfErrorRows = Math.abs(allRows.size - nonErrorRows.size)
        if numberOfErrorRows > 0 then
          val errorIndices = Range(numberOfErrorRows, allRows.size + 1)
          val newIDs = errorIndices.map: index =>
            s"${if id(id.length - 2) == '.' then id else s"$id.0"}-E$index"
          val oldToNew = nonErrorRows.map(_.id).zip(newIDs)
          oldToNew.foreach: item =>
            getRepo.replaceIDByPrimaryKey(item._1, item._2)
        end if
      end if
  end markAllRowsWithIDAsError

  def markAllRowsMatchingRecordsWithIDAsError[EC <: ItsaEC : ClassTag](ecs: EC*): Unit =
    transact(dataSource):
      ecs.foreach:
        case ecID: &[HLEntityCreator, ItsaIDOfSomeKind] => markAllRowsWithIDAsError(ecID.getItsaID)


  def addWipingRecords(disks: WipingCreator*): Unit =
    given table: HLTableInfo[WipingCreator, Wiping] = tables.wipingTable
    given ct: ClassTag[WipingCreator] = classTag[WipingCreator]
    // This supports up to 702 drives with the same ID prefix, e.g. "HDD1234A", "HDD1234B", ..., "HDD1234ZZ"
    def getSuffix(start: Int, current: Int): String =
      val index = start + current
      if index > lettersInTheAlphabet then
        val times = index / lettersInTheAlphabet
        s"${letters(times - 1)}${letters(index % lettersInTheAlphabet)}"

      else
        letters(index).toString
    end getSuffix

    logger.info(s"Wiping disks: $disks")
    val foundDrives: Seq[Wiping] = findAllByIdStartingWith(disks.head.hddID) ?? Seq.empty[Wiping]
    val suffixStartIndex = if foundDrives.size > 1 then foundDrives.size else 0
    val newDisks: Seq[WipingCreator] =
      if suffixStartIndex > 0 then
        logger.info(s"Found ${foundDrives.size} drives with the same ID prefix: ${disks.head.hddID}")
        disks.indices.map: index =>
          val suffix = getSuffix(suffixStartIndex, index)
          disks(index).copy(hddID = disks.head.hddID + suffix)

      else
        logger.info(s"No drives found with the same ID prefix: ${disks.head.hddID}")
        disks.indices.map: index =>
          val suffix = if index == 0 then "" else getSuffix(0, index - 1)
          disks(index).copy(hddID = disks.head.hddID + suffix)
    end newDisks


    // Supports up to ZZ (702 letters) for the hddID prefix
    //    val newDisks: Seq[WipingCreator] =
    //      val foundDrives: Seq[WipingCreator] = findAllByIdStartingWith[WipingCreator](disks.head.hddID).map(_.map(WipingCreator.apply)) ?? Seq.empty[WipingCreator]
    //      val suffixStartIndex = if foundDrives.size > 1 then foundDrives.size else 0
    //      if foundDrives.size == 0 then
    //        // If no drives found, we can use the first drive as is
    //        disks.toSeq
    //      else if foundDrives.size > lettersInTheAlphabet then
    //        // If more than 26 drives found, we need to add a prefix
    //        val times = foundDrives.size / lettersInTheAlphabet
    //        val prefix = letters(times - 1) + letters(foundDrives.size % lettersInTheAlphabet)
    //        disks.map(_.copy(hddID = disks.head.hddID + prefix))
    //      else if foundDrives.size == 1 then
    //        // If only one drive found, we can use the first drive as is
    //        disks.map(_.copy(hddID = disks.head.hddID + letters(suffixStartIndex)))
    //      else
    //        // If multiple drives found, we need to add a prefix to each drive
    //        disks.indices.map: index =>
    //          disks(index).copy(hddID = disks.head.hddID + letters(suffixStartIndex + disks.indexOf(index)))
    //
    ////      boundary:
    //        val allDisks: Seq[WipingCreator] =
    //          if disks.size == 1 then
    //            val foundDisks: Seq[WipingCreator] = findAllByIdStartingWith[WipingCreator](disks.head.hddID) match
    //              case Some(value) => value.map(WipingCreator.apply)
    //              case None => Seq.empty[WipingCreator]
    //            if foundDisks.isEmpty then
    //              if foundDisks.size > lettersInTheAlphabet then
    //                val times = foundDisks.size / lettersInTheAlphabet
    //                val prefix = letters(times - 1) + letters(foundDisks.size % lettersInTheAlphabet)
    //                Seq(disks.head.copy(hddID = disks.head.hddID + prefix))
    //              else
    //                Seq(disks.head.copy(hddID = disks.head.hddID + letters(foundDisks.size)))
    //            else
    //              disks
    //          else
    //            disks
    //        end allDisks
    //        if allDisks != disks then
    //          boundary.break(allDisks)

    //        val noIDDrives = disks.groupBy(_.hddID) //filter(d => d.hddID.isEmpty || d.hddID == "NOT LOGGED")
    //        noIDDrives.values.flatMap: driveGroup =>
    //          if driveGroup.size >= 2 then
    //            val drives = driveGroup.drop(1)
    //            val first = drives.head
    //            val mappedDisks = (1 until drives.size).map: index =>
    //              val i = index - 1 // Start from 0
    //              val times = if i > 26 then i / lettersInTheAlphabet else 0
    //              val prefix = if times > 0 then letters(times - 1) + letters(i) else letters(i)
    //              val newDisk = drives(index).copy(hddID = drives(index).hddID + prefix)
    //              logger.info(s"New Disk: $newDisk")
    //              newDisk
    //            Seq(first) ++ mappedDisks
    //          else
    //            disks.toSeq
    //        .toSeq
    transact(dataSource):
      markAllRowsMatchingRecordsWithIDAsError(newDisks *)
      val diskToAdd = newDisks
        .dropWhile(_.hddID in foundDrives.map(_.hddID))
      diskToAdd.foreach(repos.wipingRepo.insert)
    .get


  def findWipingRecord(serial: String): Option[Wiping] =
    given table: HLTableInfo[WipingCreator, Wiping] = tables.wipingTable

    given wipingCreatorCT: ClassTag[WipingCreator] = classTag[WipingCreator]

    transact(dataSource):
      repos.wipingRepo.findWipingRecord(serial)
    .toOptionFlat
  end findWipingRecord

  /**
   * This method will only replace the ID in the database, not in the entity itself, if there is an entity with the old ID.
   *
   * @param old   The old ID to be replaced
   * @param `new` The new ID to replace the old one
   * @tparam EC The type of the {{H
   */

  def replaceAllRowsWithID[EC <: ItsaEC : ClassTag](old: String, `new`: String): Unit =

    transact(dataSource):
      given tableInfo: HLTableInfo[EC, EntityFromEC[EC]] = getTableInfo[EC, EntityFromEC[EC]]
      getRepo.replaceIdWith(old, `new`)

  def findWipingRecordID(serial: String): Option[String] =
    findWipingRecord(serial).map(_.hddID)


  def findByID[EC <: ItsaEC : ClassTag](id: String): Option[EntityFromEC[EC]] =
    transact(dataSource):
      given tableInfo: HLTableInfo[EC, EntityFromEC[EC]] = getTableInfo[EC, EntityFromEC[EC]]
      getRepo.findAllByID(id)(using summon[DbCon], getDbCodec).headOption
    .toOptionFlat

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
          db
        case org.itsadigitaltrust.common.Result.Error(_) =>
          //          else
          Result.error(Error.ConnectionError)
  //        case org.itsadigitaltrust.common.Result.Error(error) => Result.error(Error.LoaderError(error))


end HLDatabase





