package org.itsadigitaltrust.hardwarelogger.backend

import com.augustnagro.magnum.{DbCodec, DbCon, DbTx, TableInfo, transact as magTransact}
import com.mysql.cj.exceptions.MysqlErrorNumbers
import com.mysql.cj.jdbc.MysqlDataSource
import org.itsadigitaltrust.common.Operators.*
import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.hardwarelogger.backend
import org.itsadigitaltrust.hardwarelogger.backend.HLDBErrorCode
import org.itsadigitaltrust.hardwarelogger.backend.backend.*
import org.itsadigitaltrust.hardwarelogger.backend.entities.ItsaIDOfSomeKind
import ox.either.{fail, ok}

import java.sql.SQLException
import javax.sql.DataSource
import scala.compiletime.summonInline
import scala.reflect.{ClassTag, TypeTest, Typeable, classTag}
import scala.util.{Failure, Success, Try, Using}


private final class HLDatabaseLoaderService(propertiesFilePath: String) extends DataSourceLoaderService(propertiesFilePath):
  override def dataSource: Option[MysqlDataSource] =
    apply(propertiesFilePath).toOption

class HLDatabase(private val configFile: String)(using dataSourceLoader: DataSourceLoaderService) extends HWLLoggable:
  type RepoType[EC <: HLEntityCreator] = HLRepo[EC, EntityFromEC[EC]]

  import repos.given
  import tables.given

  private def handleMySqlErrorCode(errorCode: Int): HLDBErrorCode = errorCode match
    case MysqlErrorNumbers.ER_DUP_ENTRY => HLDBErrorCode.EntryAlreadyExists
    case MysqlErrorNumbers.ER_ACCESS_DENIED_ERROR_WITH_PASSWORD => HLDBErrorCode.ConnectionError("Password")
    case MysqlErrorNumbers.ER_ACCESS_DENIED_ERROR => HLDBErrorCode.ConnectionError("Username")
    case MysqlErrorNumbers.ER_BAD_HOST_ERROR => HLDBErrorCode.ConnectionError("Address")
    case _ => HLDBErrorCode.UnknownError

  private def transact[T](connection: DataSource)(f: DbTx ?=> T | HLDBTransactionResult[T]): HLDBTransactionResult[T] =
    Try(magTransact(connection)(f)) match
      case Failure(exception: SQLException) =>
        logger.error("Error: " + exception.getLocalizedMessage)
        Left(handleMySqlErrorCode(exception.getErrorCode))
      case Success(value: T) => Right(value)
      case Success(value: HLDBTransactionResult[T]) => value
      case scala.util.Failure(_) => Left(HLDBErrorCode.ConnectionError("Unknown"))


  end transact


  private def dataSource =
    if dataSourceLoader.dataSource.isDefined then
      dataSourceLoader.dataSource.get
    else
      dataSourceLoader(configFile)
      dataSourceLoader.dataSource.get

  private lazy val letters = 'A' to 'Z'
  private final val lettersInTheAlphabet = 26

  private given repo: [EC <: HLEntityCreator : ClassTag, E <: HLEntity: ClassTag] => HLRepo[EC, E] = givenRepo[EC, E]


  private inline def getTableInfo[EC <: HLEntityCreator : ClassTag, E <: HLEntity]: HLTableInfo[EC, E] =
    logger.info(s"Class Tag: ${summonInline[ClassTag[EC]]}")
    val result = classTag[EC] match
      case c if c == classTag[MemoryCreator] => tables.memoryTable
      case c if c == classTag[MediaCreator] => tables.mediaTable
      case c if c == classTag[DiskCreator] => tables.diskTable
      case c if c == classTag[InfoCreator] => tables.infoTable
      case c if c == classTag[WipingCreator] => tables.wipingTable
      case _ => throw new IllegalArgumentException(s"Unknown entity creator class: ${summonInline[ClassTag[EC]]}")
    result.asInstanceOf[HLTableInfo[EC, E]]


  private transparent inline def getRepo[EC <: ItsaEC](creator: EC): RepoType[EC] =
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
    result.asInstanceOf[RepoType[EC]]
  end getRepo

  private transparent inline def getRepo[EC <: ItsaEC : ClassTag] =
    summonInline[ClassTag[EC]] match
      case c if c == classTag[MemoryCreator] => repos.memoryRepo
      case c if c == classTag[MediaCreator] => repos.mediaRepo
      case c if c == classTag[DiskCreator] => repos.diskRepo
      case c if c == classTag[InfoCreator] => repos.infoRepo
      case c if c == classTag[WipingCreator] => repos.wipingRepo
  end getRepo

  given table: [EC <: HLEntityCreator : ClassTag, E <: HLEntity] => HLTableInfo[EC, E] = getTableInfo
  given dbCodec: [EC <: HLEntityCreator : ClassTag, E <: HLEntity] => DbCodec[E] = getDbCodec

  private def getClassTagForEntityTypeEC[EC <: ItsaEC : ClassTag]: ClassTag[EntityFromEC[EC]] =
    summon[ClassTag[EntityClassTagFromEC[EC]]].asInstanceOf[ClassTag[EntityFromEC[EC]]]
  end getClassTagForEntityTypeEC


  def testConnection(): Boolean =
    Using(dataSource.getConnection)(_.isValid(1)).getOrElse(false)


  def doesGenSerialExist(genSerial: String): HLDBTransactionResult[Boolean] =
    transact(dataSource):
      repos.infoRepo.doesGenSerialExist(genSerial)

  def insertOrUpdate[EC <: HLEntityCreator : ClassTag, E <: HLEntity](creator: EC): HLDBTransactionResult[Unit] =

    transact(dataSource):
      repo.insertOrUpdate(creator)(using summon[DbCon])


  def findAllByID[EC <: HLEntityCreator : ClassTag, E <: HLEntity : ClassTag ](id: String): HLDBTransactionResult[Vector[Option[E]]] =
    transact(dataSource):
      repo.findAllByItsaID(id)


  def findAllByIdStartingWith[E <: HLEntity : ClassTag, EC <: HLEntityCreator : ClassTag](id: String): HLDBTransactionResult[Vector[E]] =
    transact(dataSource):
      given dbCodec: DbCodec[E] = getDbCodec
      val vec: Vector[Option[E]] = repo.findAllByIDStartingWith(id)

      if vec.isEmpty then
        Left(HLDBErrorCode.NoEntriesFound)
      else
        vec.flatMap: item =>
          item.map(e => e)
        .? ?? Left(HLDBErrorCode.NoEntriesFound)

  end findAllByIdStartingWith


  def addPCInfo(info: InfoCreator): HLDBTransactionResult[Unit] =
    ox.either:
      info.genSerial.map: serial =>
        val exists = doesGenSerialExist(serial).ok()
        if exists then
          HLDBErrorCode.EntryAlreadyExists.fail()
        else
          insertOrUpdate(info)


  def markAllRowsWithIDAsError[EC <: HLEntityCreatorWithItsaID | HLEntityCreatorWithHardDiskID : ClassTag, E <: HLEntity : ClassTag](id: String): Unit =
    transact(dataSource):

      given dbCodec: DbCodec[E] = getDbCodec
      logger.info(s"Marking rows with id: $id as error!")
      if id == null || id.isEmpty then
        ()
      else
        val nonErrorRows = repo.findAllByItsaID(id)
        val allRows = repo.findAllByIDStartingWith(id) or Vector.empty[Option[E]] |> Option.apply
        val numberOfErrorRows = Math.abs(allRows.size - nonErrorRows.size)
        if numberOfErrorRows > 0 then
          val errorIndices = Range(numberOfErrorRows, allRows.size + 1)
          val newIDs = errorIndices.map: index =>
            s"${if id.length >= 2 && id(id.length - 2) == '.' then id else s"$id.0"}-E$index"
          val oldToNew = nonErrorRows.flatten.map(_.id).zip(newIDs)
          oldToNew.foreach: item =>
            repo.replaceIDByPrimaryKey(item._1, item._2)(using summon[DbCon])
        end if
      end if
  end markAllRowsWithIDAsError

  def findWipingRecord(serial: String): HLDBTransactionResult[Option[Wiping]] =
    given table: HLTableInfo[WipingCreator, Wiping] = tables.wipingTable

    given wipingCreatorCT: ClassTag[WipingCreator] = classTag[WipingCreator]

    transact(dataSource):
      repos.wipingRepo.findWipingRecord(serial)

  end findWipingRecord

  def addWipingRecords(disks: WipingCreator*): HLDBTransactionResult[Unit] =
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
    transact(dataSource):
      val result: HLDBTransactionResult[Vector[EntityFromEC[ECFromEntity[Wiping]]]] = findAllByIdStartingWith[Wiping, ECFromEntity[Wiping]](disks.head.hddID)

      ox.either:
        val foundDrives: Vector[EntityFromEC[ECFromEntity[Wiping]]] = result.ok()

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

        given ct: ClassTag[WipingCreator] = classTag[WipingCreator]

        markAllRowsMatchingRecordsWithIDAsError[WipingCreator, Wiping](newDisks *)



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
  //      eitherTransact[Seq[WipingCreator], Wiping,WipingCreator](dataSource,HLDBErrorCode.UnknownError,wipingTable):
  //        markAllRowsMatchingRecordsWithIDAsError(newDisks*)
  //      .get


  end addWipingRecords

  def doesDriveExists(creator: DiskCreator): HLDBTransactionResult[Boolean] =
    transact(dataSource):
      repos.diskRepo.sameDriveWithSerialNumber(creator.serial) match
        case Nil => false
        case _ => true
  end doesDriveExists


  def findItsaIdByGenSerialNumber(serial: String): HLDBTransactionResult[String] =
    transact(dataSource):
      infoRepo.findItsaIdBySerialNumber(serial) match
        case Some(value) => Right(value)
        case None => Left(HLDBErrorCode.NoEntriesFound)


  import scala.reflect.Selectable.reflectiveSelectable

  @throws[IllegalArgumentException]("Unknown entity creator class")
  private transparent inline def givenRepo[EC <: HLEntityCreator: ClassTag, E <: HLEntity]:
  HLRepo[EC, E] = classTag[EC] match
    case c if c == classTag[MemoryCreator] => repos.memoryRepo.asInstanceOf[HLRepo[EC, E]]
    case c if c == classTag[MediaCreator] => repos.mediaRepo.asInstanceOf[HLRepo[EC, E]]
    case c if c == classTag[DiskCreator] => repos.diskRepo.asInstanceOf[HLRepo[EC, E]]
    case c if c == classTag[InfoCreator] => repos.infoRepo.asInstanceOf[HLRepo[EC, E]]
    case c if c == classTag[WipingCreator] => repos.wipingRepo.asInstanceOf[HLRepo[EC, E]]
    case _ => throw new IllegalArgumentException(s"Unknown entity creator class: ${summonInline[ClassTag[EC]]}")

  private inline def getDbCodec[EC <: HLEntityCreator : ClassTag, E <: HLEntity] =
    val result = summon[ClassTag[EC]] match
      case c if c == classTag[MemoryCreator] => summon[DbCodec[Memory]]
      case c if c == classTag[MediaCreator] => summon[DbCodec[Media]]
      case c if c == classTag[DiskCreator] => summon[DbCodec[Disk]]
      case c if c == classTag[InfoCreator] => summon[DbCodec[Info]]
      case c if c == classTag[WipingCreator] => summon[DbCodec[Wiping]]
      case c if c == classTag[HLEntityCreatorWithHardDiskID] => summon[DbCodec[Wiping]]
    result.asInstanceOf[DbCodec[E]]
  end getDbCodec
  def markAllRowsMatchingRecordsWithIDAsError[EC <: HLEntityCreatorWithItsaID | HLEntityCreatorWithHardDiskID : ClassTag, E <: HLEntity : ClassTag](ecs: EC*): Unit =


    transact(dataSource):
      ecs.map(_.getItsaID).foreach(markAllRowsWithIDAsError[EC, E])


  def findByID[EC <: HLEntityCreatorWithItsaID | HLEntityCreatorWithHardDiskID : ClassTag, E <: HLEntity ](itsaID: String): HLDBTransactionResult[E] =
    transact(dataSource):

      given tableInfo: HLTableInfo[EC, E] = getTableInfo[EC, E]

      given dbCon: DbCon = summon[DbCon]

      // first
      val result: Option[E] = repo.findAllByItsaID(itsaID)(0).asInstanceOf[Option[E]]

      result ?? Left(HLDBErrorCode.NoEntriesFound)


  def close(): Unit = ()
end HLDatabase

object HLDatabase:
  enum Error:
    case LoaderError(error: DataSourceLoaderService.Error)
    case ConnectionError extends Error

  given Conversion[DataSourceLoaderService.Error, Error] with
    override def apply(x: DataSourceLoaderService.Error): Error =
      Error.LoaderError(x)

  def apply(dbProperties: => String, testConnection: Boolean = true)(using dataSourceLoaderFactory: DataSourceLoaderFactory = DataSourceLoader.dataSourceLoaderFactory): Either[Error, HLDatabase] =
    given dataSourceLoader: DataSourceLoaderService = dataSourceLoaderFactory(dbProperties)

    ox.either:
      val db =
        new HLDatabase(dbProperties)

      if !(testConnection && db.testConnection()) then
        db.close()
        Error.ConnectionError.fail()
      else db
  end apply
end HLDatabase





