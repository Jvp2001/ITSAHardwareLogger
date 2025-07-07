package org.itsadigitaltrust.hardwarelogger.backend


import org.itsadigitaltrust.common.Operators.in
import org.itsadigitaltrust.hardwarelogger.backend.entities.*
import org.itsadigitaltrust.hardwarelogger.backend.tables.HLTableInfo
import org.itsadigitaltrust.hardwarelogger.backend.types.{EntityFromEC, ItsaEC}

import java.sql.Timestamp
import java.time.OffsetDateTime
import scala.reflect.{ClassTag, classTag}
import com.augustnagro.magnum.*
extension [EC, E, ID](table: TableInfo[EC, E, ID])
  def hasColumn(scalaName: String): Boolean =
    table.all.columnNames.exists(_.scalaName == scalaName)

type ECTableInfo[EC <: ItsaEC] = TableInfo[EC, EntityFromEC[EC], Long]

private[backend] object repos:
  type HLRepo[EC, E] = Repo[EC, E, Long]
  inline def HLRepo[EC <: ItsaEC : ClassTag, E <: EntityFromEC[EC]](using RepoDefaults[EC, E, Long]): HLRepo[EC, E] = Repo[EC, E, Long]

  extension [EC <: ItsaEC, E <: EntityFromEC[EC]](repo: HLRepo[EC, E])(using ClassTag[EC])

    /**
     * Returns the name of the scala field that corresponds to the ITSA ID column in the database.
     * @param table The table that is being queried.
     * @return "itsaID" if the table has a column named "itsaID", otherwise "hddID".
     */
    private def idScalaName(using table: HLTableInfo[EC, E]): "itsaID" | "hddID" =
      if table.hasColumn("itsaID") then "itsaID" else "hddID"

    private def getItsaIDFieldName: String = if classTag[EC].runtimeClass == classOf[WipingCreator] then
      "hddID"
      else
      "itsaID"
    private[backend] def findAllByID(id: String)(using DbCon, DbCodec[EntityFromEC[EC]])(using table: HLTableInfo[EC, E]): Seq[EntityFromEC[EC]] =

      val frag = sql"select * from $table where ${table.selectDynamic(getItsaIDFieldName)} = $id"
      val result  = frag.query.run()
      result
    end findAllByID


    private[backend] def findAllByIdsStartingWith(id: String)(using DbCon, DbCodec[EntityFromEC[EC]])(using table: HLTableInfo[EC, E]): Option[Seq[EntityFromEC[EC]]] =

      // Either itsaid or hdd_id

      val frag = sql"select * from $table where ${table.selectDynamic(getItsaIDFieldName)} like '$id%'"
      Option(frag.query.run())

    private[backend] def replaceIdWith(old: String, `new`: String)(using DbCon)(using table: HLTableInfo[EC, E]): Unit =
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

    def insertOrUpdate(creator: EC)(using DbCon)(using table: HLTableInfo[EC, E]): Unit =
      creator match
        case infoCreator: InfoCreator =>
          System.out.println(infoCreator.itsaID)
          sql"select itsaid from info where itsaid = ${infoCreator.itsaID}".query[String].run().headOption match
            case Some(info) => ()
                          sql"update $table set lastupdated = ${Timestamp.from(OffsetDateTime.now().toInstant)} where itsaid = ${infoCreator.itsaID}".update.run()
            case None => repo.insert(creator)
          end match
        case c: HLEntityCreatorWithItsaID =>
          val result = sql"select itsaid from $table where itsaid = ${c.itsaID}".query[String].run()
          result.headOption match
            case Some(info) => ()
            case None =>
              repo.insert(creator)
          end match
        case _: org.itsadigitaltrust.hardwarelogger.backend.entities.
        HLEntityCreatorWithHardDiskID => ()
      end match
    end insertOrUpdate

  end extension


  given diskRepo: HLRepo[DiskCreator, Disk] = HLRepo[DiskCreator, Disk]

  given wipingRepo: HLRepo[WipingCreator, Wiping] = HLRepo[WipingCreator, Wiping]

  given mediaRepo: HLRepo[MediaCreator, Media] = HLRepo[MediaCreator, Media]

  given infoRepo: HLRepo[InfoCreator, Info] = HLRepo[InfoCreator, Info]

  given memoryRepo: HLRepo[MemoryCreator, Memory] = HLRepo[MemoryCreator, Memory]


  extension (repo: HLRepo[InfoCreator, Info])
    def findItsaIdBySerialNumber(serial: String)(using DbCon)(using table: HLTableInfo[InfoCreator, Info]): Option[String] =
      val frag = sql"select itsaid from $table where ${table.selectDynamic("genSerial")} = $serial"
      frag.query[String].run().headOption
  end extension

  extension (repo: HLRepo[DiskCreator, Disk])
    def sameDriveWithSerialNumber(serial: String)(using DbCon)(using table: HLTableInfo[DiskCreator, Disk]): Seq[Disk] =
      val frag = sql"select * from $table where ${table.selectDynamic("serial")} = $serial"
      frag.query[Disk].run()
  end extension

  extension (repo: HLRepo[WipingCreator, Wiping])
    private[backend] def findWipingRecord(serial: String)(using DbCon)(using table: HLTableInfo[WipingCreator, Wiping]): Option[Wiping] =
      val frag = sql"select * from $table where ${table.selectDynamic("serial")} = $serial"
      frag.query[Wiping].run().headOption

    private[backend] def getLatestNoIDValue(using DbCon): Option[String] =
      sql"select MAX(itsahw.wiping.hdd_id) from wiping".query[String].run().headOption
  end extension
end repos


//
//  extension(r: HLRepo[MemoryCreator, Memory])
//    def insertOrUpdate(creator: MemoryCreator)(using table: TableInfo[MemoryCreator, Memory, Long]: Unit =
//

