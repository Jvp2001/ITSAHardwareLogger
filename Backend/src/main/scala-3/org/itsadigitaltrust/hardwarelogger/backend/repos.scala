package org.itsadigitaltrust.hardwarelogger.backend

import com.augustnagro.magnum.*
import org.itsadigitaltrust.common.Operators.{??, in, |>}
import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.common.toVectorOfOptions
import org.itsadigitaltrust.hardwarelogger.backend.HLDBErrorCode.EntryAlreadyExists
import org.itsadigitaltrust.hardwarelogger.backend.database.RepoInstancesContainer
import org.itsadigitaltrust.hardwarelogger.backend.entities.*
import org.itsadigitaltrust.hardwarelogger.backend.repos.HLRepoWithID
import ox.either.{fail, ok}

import java.sql.Timestamp
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.reflect.{ClassTag, classTag}
import scala.compiletime.deferred
import scala.util.boundary.Label
import scala.util.matching.Regex

extension [EC, E, ID](table: TableInfo[EC, E, ID])
  def hasColumn(scalaName: String): Boolean =
    table.all.columnNames.exists(_.scalaName == scalaName)

type ECTableInfo[EC <: HLEntityWithSomeKindOfID] = TableInfo[EC, ? <: HLEntityWithSomeKindOfID, Long]


object repos extends RepoInstancesContainer:

  given diskRepo: DiskRepo = new DiskRepo()

  given wipingRepo: WipingRepo = WipingRepo()

  given mediaRepo: MediaRepo = MediaRepo()

  given infoRepo: InfoRepo = InfoRepo()

  given memoryRepo: MemoryRepo = MemoryRepo()

  trait HLRepo[EC <: HLEntityCreator : ClassTag, E <: HLEntity] extends Repo[EC, E, Long], HWLLoggable:
    given table: TableInfo[EC, E, Long] = deferred

  trait HLRepoWithID[EC <: HLEntityCreatorWithSomeKindOfID : {ClassTag, DbCodec}, E <: HLEntityWithSomeKindOfID : DbCodec] extends HLRepo[EC, E]:


    protected final val itsaIDErrorSuffixPattern: Regex = ".*-E[0-9]+$".r
    


    def findAllByIDSuffixedWith(creator: EC, suffix: String)(using DbCon): Vector[Option[E]] =
      val frag = sql"select * from $table where ${table.selectDynamic(getItsaIDFieldName)} like '${creator.getItsaID}%$suffix"
      frag.query[E].run().toVectorOfOptions

    def findAllByIDStartingWith(id: String)(using DbCon): Vector[Option[E]] =

      // Either itsaid or hdd_id
      val idColName = getItsaIDFieldName


      val frag = sql"select * from $table where ${table.selectDynamic(getItsaIDFieldName)} like '$id%'"
      print(frag)
      frag.query[E].run().toVectorOfOptions
    end findAllByIDStartingWith

    def findAllByIDEndingWith(id: String)(using DbCon): Vector[Option[E]] =
      val frag = sql"select * from $table where ${table.selectDynamic(getItsaIDFieldName)} like '%$id'"
      frag.query[E].run().toVectorOfOptions
    end findAllByIDEndingWith
    
    def replaceIdWith(old: String, `new`: String)(using DbCon)(using table: HLTableInfo[EC, E]): Unit =
      val scalaName = (idScalaName, old)
      val frag =
        sql"""update $table set ${table.selectDynamic(scalaName._1)} = ${`new`}
               where ${table.selectDynamic(scalaName._1)} = $scalaName._2"""
      frag.update.run()
    end replaceIdWith

    /**
     * Returns the name of the scala field that corresponds to the ITSA ID column in the database.
     *
     * @param table The table that is being queried.
     * @return "itsaID" if the table has a column named "itsaID", otherwise "hddID".
     */
    private def idScalaName(using table: HLTableInfo[EC, E]): "itsaID" | "hddID" =
      if table.hasColumn("itsaID") then "itsaID" else "hddID"

    def replaceIDByPrimaryKey(key: Long, `new`: String)(using DbCon)(using table: HLTableInfo[EC, E]): Unit =
      val frag =
        sql"""update $table set ${table.selectDynamic(idScalaName)} = ${`new`}
               where ${table.selectDynamic(getPrimaryKeyColumnName)} = $key"""
      frag.update.run()
    end replaceIDByPrimaryKey

    private def getPrimaryKeyColumnName: "int" | "id" =
      if classOf[HLEntityWithHardDiskID] in classTag[EC].runtimeClass.getInterfaces then
        "int"
      else
        "id"
    end getPrimaryKeyColumnName

    def updateItsaIDFromCreator(oldItsaID: String, newItsaID: String)(using DbCon): Unit =
      val frag = sql"update $table set ${table.selectDynamic(getItsaIDFieldName)} = ${newItsaID} where ${table.selectDynamic(getItsaIDFieldName)} = $oldItsaID"
      frag.update.run()


    def updateItsaID(oldItsaID: String, entity: E)(using DbCon): Unit =
      val frag = sql"update $table set ${table.selectDynamic(getItsaIDFieldName)} = ${entity.getItsaID} where ${table.selectDynamic(getItsaIDFieldName)} = $oldItsaID"
      frag.update.run()




    
    def findByItsaID(itsaID: String)(using DbCon): Option[E] =
      findAllByItsaID(itsaID).headOption.flatten


    /**
     * Some tables allow multiple rows with the same ITSA ID (for example, the memory table).
      * @param itsaID the ITSA ID to find 
     * @param x$2 the database connection
     * @return All the rows matching the [[itsaID]] exactly.
     */  
    def findAllByItsaID(itsaID: String)(using DbCon): Vector[Option[E]] =
//      sql"""select * from $table where ${table.selectDynamic(getItsaIDFieldName)} regexp  ${s"^$itsaID[A-Z]?$"}""".query[E].run().headOption
      sql"""select * from $table where ${table.selectDynamic(getItsaIDFieldName)} = $itsaID""".query[E].run().toVectorOfOptions
      
    protected def getItsaIDFieldName: String =
      if summon[ClassTag[EC]] == classTag[WipingCreator] then
        logger.info("ItsaID Field name: hddID")
        "hddID"
      else
        logger.info("ItsaID Field name: itsaID")
        "itsaID"

    def findFirstByField(fieldName: String)(using DbCon): Option[E] =
      findAllByField(fieldName).head

    def findAllByField(fieldName: String)(using DbCon, DbCodec[E]): Vector[Option[E]] =
      sql"""select * from $table where ${table.selectDynamic(fieldName)}""".query.run().toVectorOfOptions

    def findAllWithErrorNumber(withID: String)(using DbCon, DbCodec[Info]): Vector[Info] =
      val frag = sql"""select * from info where itsaid regexp ${s"$withID-E\\d+"}"""
      frag.query[Info].run().toVectorOfOptions.flatten
    end findAllWithErrorNumber
    
    def getAllWithErrorSuffix(withID: String)(using DbCon, DbCodec[Info]): Long =
      findAllWithErrorNumber(withID).count(_ != null)

    def markRowAsError(itsaID: String)(using DbCon): HLDBTransactionResult[Unit] =
      ox.either:
        val entity = findByItsaID(itsaID) ?? HLDBErrorCode.NoEntriesFound.fail()
        val totalErrorCount = getAllWithErrorSuffix(itsaID)
        
        this.updateItsaID(s"$itsaID-E$totalErrorCount", entity)
    end markRowAsError
    
    def deleteByItsaID(itsaID: String)(using DbCon): Int =
      val frag = sql"delete from $table where ${table.selectDynamic(getItsaIDFieldName)} = $itsaID"
      frag.update.run()
    end deleteByItsaID
    

  end HLRepoWithID

  trait HLRepoWithSerial[EC <: HLEntityCreatorWithSomeKindOfID : DbCodec, E <: HLEntityWithSomeKindOfID] extends HLRepoWithID[EC, E]:

    lazy val serialColumnScalaName: String = "genSerial"

    def findItsaIDBySerialNumber(serial: String)(using DbCon, DbCodec[E]): Option[String] =
      findBySerialNumber(serial).map(_.getItsaID)

    
    def findBySerialNumber(serial: String)(using DbCon, DbCodec[E]): Option[E] =
      // Prefer entries whose itsaID does NOT end with an error suffix like -E<number>
      val nonErrorPattern = itsaIDErrorSuffixPattern
      val nonErrorFrag = sql"select * from $table where ${table.selectDynamic(serialColumnScalaName)} = $serial and ${table.selectDynamic(getItsaIDFieldName)} not regexp ${nonErrorPattern.toString}"
      val nonErrorResult = nonErrorFrag.query[E].run().headOption
      if nonErrorResult.nonEmpty then nonErrorResult
      else
        val frag = sql"select * from $table where ${table.selectDynamic(serialColumnScalaName)} = $serial"
        frag.query[E].run().headOption
    end findBySerialNumber
    
    def updateItsaIDBySerial(entityCreator: EC)(using DbCon): Unit =
      val frag = sql"update $table set itsaid = ${table.selectDynamic("itsaID")} where ${table.selectDynamic(serialColumnScalaName)} = ${table.selectDynamic(serialColumnScalaName)}"
      frag.update.run()
    end updateItsaIDBySerial

    def deleteBySerialIfItsaIDDoesNotMatch(serial: String, itsaID: String)(using DbCon): Int =
      val frag = sql"delete from $table where ${table.selectDynamic(serialColumnScalaName)} = $serial and itsaid != $itsaID"
      frag.update.run()
    
    def deleteBySerial(serial: String)(using DbCon): Int = 
      val frag = sql"delete from $table where ${table.selectDynamic(serialColumnScalaName)} = $serial"
      frag.update.run()
    
  end HLRepoWithSerial
  class InfoRepo extends HLRepoWithSerial[InfoCreator, Info]:
    given infoTable: HLTableInfo[InfoCreator, Info] = summon[HLTableInfo[InfoCreator, Info]]
    


    def updateItsaID(entityCreator: InfoCreator, newItsaID: String)(using DbCon): Unit =
      findByItsaID(entityCreator.itsaID).foreach: entity =>
        entity.copy(itsaID = newItsaID) |> this.update



    def doesGenSerialExist(serial: String)(using DbCon): Boolean =
      val result = sql"select genserial from info where genserial =  ${table.selectDynamic("genSerial")}".query[Int].run().head > 0
      result


  end InfoRepo

  class DiskRepo extends HLRepoWithSerial[DiskCreator, Disk]:
    override lazy val serialColumnScalaName: String = "serial"
    
    given diskTable: HLTableInfo[DiskCreator, Disk] = summon[HLTableInfo[DiskCreator, Disk]]

    def sameDriveWithSerialNumber(serial: String)(using DbCon, DbCodec[Disk])(using table: HLTableInfo[DiskCreator, Disk]): Seq[Disk] =
      val frag = sql"select * from $table where ${table.selectDynamic("serial")} = $serial"
      frag.query[Disk].run()

    def findDiskRecord(serial: String)(using DbCon)(using table: HLTableInfo[DiskCreator, Disk]): Option[Disk] =
      // Prefer non-error itsaID entries first
      val nonErrorPattern = ".*-E[0-9]+$"
      val nonErrorFrag = sql"select * from $table where ${table.selectDynamic("serial")} = $serial and ${table.selectDynamic(getItsaIDFieldName)} not regexp $nonErrorPattern"
      val nonError = nonErrorFrag.query[Disk].run().headOption
      if nonError.nonEmpty then nonError
      else
        val frag = sql"select * from $table where ${table.selectDynamic("serial")} = $serial"
        frag.query[Disk].run().headOption
    end findDiskRecord
  
  end DiskRepo
  

  class WipingRepo extends HLRepoWithSerial[WipingCreator, Wiping]:

    override lazy val serialColumnScalaName: String = "serial"
    
    given wipingTable: HLTableInfo[WipingCreator, Wiping] = summon[HLTableInfo[WipingCreator, Wiping]]

    def findWipingRecord(serial: String)(using DbCon): Option[Wiping] =
      val frag = sql"select * from $table where ${table.selectDynamic("serial")} = $serial"
      frag.query[Wiping].run().headOption

    def getLatestNoIDValue(using DbCon): Option[String] =
      sql"select MAX($table.wiping.hdd_id) from wiping".query[String].run().headOption
  end WipingRepo

  class MemoryRepo extends HLRepoWithID[MemoryCreator, Memory]

  class MediaRepo extends HLRepoWithID[MediaCreator, Media]
end repos


//
//  extension(r: HLRepo[MemoryCreator, Memory])
//    def insertOrUpdate(creator: MemoryCreator)(using table: TableInfo[MemoryCreator, Memory, Long]: Unit =
//






