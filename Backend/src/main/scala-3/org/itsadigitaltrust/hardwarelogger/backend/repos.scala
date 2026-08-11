package org.itsadigitaltrust.hardwarelogger.backend


import com.augustnagro.magnum.*
import org.itsadigitaltrust.common.Operators.in
import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.common.{Result, toVectorOfOptions}
import org.itsadigitaltrust.hardwarelogger.backend.entities.*
import org.itsadigitaltrust.hardwarelogger.backend.types.{EntityFromEC, ItsaEC}
import ox.either.fail

import java.sql.Timestamp
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.reflect.{ClassTag, classTag}
import scala.compiletime.deferred
import scala.util.boundary.Label
extension [EC, E, ID](table: TableInfo[EC, E, ID])
  def hasColumn(scalaName: String): Boolean =
    table.all.columnNames.exists(_.scalaName == scalaName)

type ECTableInfo[EC <: ItsaEC] = TableInfo[EC, EntityFromEC[EC], Long]

 object repos:


  trait HLRepo[EC <: HLEntityCreator : ClassTag, E <: HLEntity] extends Repo[EC, E, Long]:
    given table: TableInfo[EC, E, Long] = deferred

  trait HLRepoWithID[EC <: HLEntityCreatorWithSomeKindOfID : ClassTag, E <: HLEntityWithSomeKindOfID] extends HLRepo[EC, E]:
  //    private transparent inline def dbCodec = repo match
  //      case _: HLRepo[InfoCreator, Info]  => summon[DbCodec[Info]]
  //      case _: HLRepo[DiskCreator, Disk] => summon[DbCodec[Disk]]
  //      case _: HLRepo[MediaCreator, Media] => summon[DbCodec[Media]]
  //      case _: HLRepo[MemoryCreator, Memory] => summon[DbCodec[Memory]]
  //      case _: HLRepo[WipingCreator, Wiping] => summon[DbCodec[Wiping]]


//
//    result.asInstanceOf[DbCodec[E]]

    /**
     * Returns the name of the scala field that corresponds to the ITSA ID column in the database.
     *
     * @param table The table that is being queried.
     * @return "itsaID" if the table has a column named "itsaID", otherwise "hddID".
     */
    private def idScalaName(using table: HLTableInfo[EC, E]): "itsaID" | "hddID" =
      if table.hasColumn("itsaID") then "itsaID" else "hddID"

    private def getItsaIDFieldName: String =
      if summon[ClassTag[EC]] == classTag[WipingCreator] then
        HWLLoggable.default().info("ItsaID Field name: hddID")
        "hddID"
      else
        HWLLoggable.default().info("ItsaID Field name: itsaID")
        "itsaID"

    def findAllByItsaID(id: String)(using DbCon)(using DbCodec[E])(using table: HLTableInfo[EC, E]): Vector[Option[E]] =
      val frag = sql"select * from $table where ${table.selectDynamic(getItsaIDFieldName)} = $id"
      val result = frag.query.run().map(Option.apply)
      result
    end findAllByItsaID


     def findAllByIDStartingWith(id: String)(using DbCon)(using DbCodec[E])(using table: HLTableInfo[EC, E]): Vector[Option[E]] =

      var tableName = classTag[EC].runtimeClass.getSimpleName.replace("Creator", "").toLowerCase
      // Either itsaid or hdd_id
      val idColName = getItsaIDFieldName

      tableName = if tableName == "disk" then "disks" else tableName

      val frag = sql"select * from $table where ${table.selectDynamic(getItsaIDFieldName)} like '$id%'"
      frag.query.run().toVectorOfOptions
    end findAllByIDStartingWith


     def replaceIdWith(old: String, `new`: String)(using DbCon)(using table: HLTableInfo[EC, E]): Unit =
      val scalaName = (idScalaName, old)
      val frag =
        sql"""update $table set ${table.selectDynamic(scalaName._1)} = ${`new`}
               where ${table.selectDynamic(scalaName._1)} = $scalaName._2"""
      frag.update.run()
    end replaceIdWith

    private def getPrimaryKeyColumnName: "int" | "id" =
      if classOf[HLEntityWithHardDiskID] in classTag[EC].runtimeClass.getInterfaces then
        "int"
      else
        "id"
    end getPrimaryKeyColumnName

    def replaceIDByPrimaryKey(key: Long, `new`: String)(using DbCon)(using table: HLTableInfo[EC, E]): Unit =
      val frag =
        sql"""update $table set ${table.selectDynamic(idScalaName)} = ${`new`}
               where ${table.selectDynamic(getPrimaryKeyColumnName)} = $key"""
      frag.update.run()
    end replaceIDByPrimaryKey

    //    def myInsert(creator: EC)(using con: DbCon, table: HLTableInfo[EC, E]): HLDBTransactionResult[EC, E, Unit] =
    //
    //      Using(con.connection.prepareStatement(sql"insert into $table values $creator".sqlString)): ps =>
    //        dbCodec.writeSingle(creator, ps)
    //        timed(ps.executeUpdate())
    //      match
    //        case Failure(exception) => Left(HLDBErrorException(exception.getMessage, EntryAlreadyExists(""), table, exception.getCause))
    //        case Success(value) => Right(value)
    //    end myInsert


    def insertOrUpdateAll(creators: EC*)(using DbCon)(using logger: HWLLoggable): HLDBTransactionResult[Unit] =
      logger().info(s"Inserting or updating: $creators")
      ox.either:
        creators.map: item =>
          insertOrUpdate(item).left.map(_.fail())



    def insertOrUpdate(creator: EC)(using DbCon)(using logger: HWLLoggable): HLDBTransactionResult[Unit] =
      logger().info(s"Inserting or updating: $creator")
      ox.either:
        creator match
          case infoCreator: InfoCreator =>
            logger().info(infoCreator.itsaID)
            sql"select itsaid from info where itsaid = ${infoCreator.itsaID}".query[String].run().headOption match
              case Some(info) if info != "" =>
                logger().info(s"Updating lastupdated for itsaid: ${infoCreator.itsaID}")
                sql"update $table set lastupdated = ${Timestamp.from(OffsetDateTime.now().toInstant)} where itsaid = $info".update.run()
              case Some(value) if value == "" => this.insert(creator)
              case None => this.insert(creator)
              case Some(_) => this.insert(creator)
              case null => this.insert(creator)
            end match
          case c: HLEntityCreatorWithItsaID =>
            val result = sql"select itsaid from $table where itsaid = ${c.itsaID}".query[String].run()
            result.headOption match
              case Some(info) => ()
              case None =>
                logger().info(s"Inserting new itsaid: ${c.itsaID}")
                this.insert(creator)

              case _: Option[String] => this.insert(creator)
            end match
          case _: org.itsadigitaltrust.hardwarelogger.backend.entities.
          HLEntityCreatorWithHardDiskID => ()
        end match
    end insertOrUpdate

    def findFirstByItsaID(itsaID: String)(using DbCon)(using DbCodec[E]): HLDBTransactionResult[E] =
      val query = sql"select * from $table where ${table.selectDynamic(getItsaIDFieldName)} = $itsaID"
      val result = findAllByItsaID(itsaID).head
      result.toRight(HLDBErrorCode.EntryNotFound)


  end HLRepoWithID



  given diskRepo: DiskRepo = DiskRepo()

  given wipingRepo: WipingRepo = WipingRepo()

  given mediaRepo: MediaRepo = MediaRepo()

  given infoRepo: InfoRepo = InfoRepo()

  given memoryRepo: MemoryRepo = MemoryRepo()

   class InfoRepo extends HLRepoWithID[InfoCreator, Info]:
    given infoTable: HLTableInfo[InfoCreator, Info] = summon[HLTableInfo[InfoCreator, Info]]
    def findItsaIdBySerialNumber(serial: String)(using DbCon): Option[String] =
      val frag = sql"select itsaid from info where genserial = $serial"
      frag.query[String].run().lastOption


    def doesGenSerialExist(serial: String)(using DbCon): Boolean =
      val result = sql"select genserial from info where genserial =  ${table.selectDynamic("genSerial")}".query[Int].run().head > 0
      print(s"Result = ${result}")
      result
  end InfoRepo

  class DiskRepo extends HLRepoWithID[DiskCreator, Disk]:
    given diskTable: HLTableInfo[DiskCreator, Disk] = summon[HLTableInfo[DiskCreator, Disk]]
    def sameDriveWithSerialNumber(serial: String)(using DbCon)(using table: HLTableInfo[DiskCreator, Disk]): Seq[Disk] =
      val frag = sql"select * from $table where ${table.selectDynamic("serial")} = $serial"
      frag.query[Disk].run()

    def findDiskRecord(serial: String)(using DbCon)(using table: HLTableInfo[DiskCreator, Disk]): Option[Disk] =
      val frag = sql"select * from $table where ${table.selectDynamic("serial")} = $serial"
      frag.query[Disk].run().headOption
  end DiskRepo

  class WipingRepo extends HLRepoWithID[WipingCreator, Wiping]:
    given wipingTable: HLTableInfo[WipingCreator, Wiping] = summon[HLTableInfo[WipingCreator, Wiping]]
     def findWipingRecord(serial: String)(using DbCon): Option[Wiping] =
      val frag = sql"select * from $table where ${table.selectDynamic("serial")} = $serial"
      frag.query[Wiping].run().headOption

     def getLatestNoIDValue(using DbCon): Option[String] =
      sql"select MAX(itsahw.wiping.hdd_id) from wiping".query[String].run().headOption
  end WipingRepo

  class MemoryRepo extends HLRepoWithID[MemoryCreator, Memory]

  class MediaRepo extends HLRepoWithID[MediaCreator, Media]
end repos


//
//  extension(r: HLRepo[MemoryCreator, Memory])
//    def insertOrUpdate(creator: MemoryCreator)(using table: TableInfo[MemoryCreator, Memory, Long]: Unit =
//




