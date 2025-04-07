package org.itsadigitaltrust.hardwarelogger.backend

import com.augustnagro.magnum.*
import entities.*
import entities.given

import java.sql.Timestamp
import java.time.OffsetDateTime

object repos:
  class HLRepo[EC, E](using default: RepoDefaults[EC, E, Long]) extends Repo[EC, E, Long]:
    private def getValues(creator: EC): String = creator match
      case c: MemoryCreator => s"(${c.descr}, ${c.itsaid}, ${c.size})"
      case c: MediaCreator => s"(${c.descr}, ${c.itsaid}, ${c.handle})"
      case c: DiskCreator => s"(${c.capacity}, ${c.description}, ${c.itsaid}, ${c.model}, ${c.serial}, ${c.`type`})"
      case c: InfoCreator => s"""(${c.cpuCores}, ${c.cpuDescription}, ${c.cpuProduct},
                                |${c.cpuSerial}, ${c.cpuSpeed}, ${c.cpuVendor}, ${c.cpuWidth}, ${c.genDesc}, ${c.genId}, ${c.genProduct},
                                | ${c.genSerial}, ${c.genVendor}, ${c.insertionDate}, ${c.itsaid}, ${c.lastUpdated},
                                | ${c.os}, ${c.totalMemory})""".stripMargin


    end getValues

    def insertOrUpdate(creator: EC)(using DbCon)(using table: TableInfo[EC, ?, Long]): Unit =
      creator match
        case infoCreator: InfoCreator =>
          println(infoCreator.itsaid)
          sql"select itsaid from info where itsaid = ${infoCreator.itsaid}".query[String].run().headOption match
            case Some(info) =>
              sql"update $table set lastupdated = ${Timestamp.from(OffsetDateTime.now().toInstant)} where itsaid = ${creator.asInstanceOf[InfoCreator].itsaid}".update.run()
            case None =>
              insert(creator)
        case c: HLEntityCreatorWithItsaID =>
          sql"select itsaid from $table where itsaid = ${c.itsaid}".query[String].run().headOption match
            case Some(info) => ()
            case None =>
              insert(creator)
  //      val value = sql"insert into ignore $table ${table.insertColumns} values ${getValues(creator)}"
  //      value.update.run()


  given DiskRepo: HLRepo[DiskCreator, Disk] = HLRepo[DiskCreator, Disk]

  given wipingRepo: HLRepo[WipingCreator, Wiping] = HLRepo[WipingCreator, Wiping]

  given MediaRepo: HLRepo[MediaCreator, Media] = HLRepo[MediaCreator, Media]

  given InfoRepo: HLRepo[InfoCreator, Info] = HLRepo[InfoCreator, Info]

  given MemoryRepo: HLRepo[MemoryCreator, Memory] = HLRepo[MemoryCreator, Memory]

  private[backend] type ItsaIDRepo = MemoryCreator | DiskCreator | InfoCreator | MediaCreator | HLEntityCreatorWithItsaID
  type ItsaEntityType[EC <: ItsaIDRepo] =
    EC match
      case MediaCreator => Media
      case InfoCreator => Info
      case MemoryCreator => Memory
      case DiskCreator => Disk
      case HLEntityCreatorWithItsaID => EC
  
  extension(repo: HLRepo[DiskCreator, Disk])
    def sameDriveWithSerialNumber(serial: String)(using DbCon)(using table: TableInfo[DiskCreator, Disk, Long]): Seq[Disk] =
      val frag = sql"select * from $table where ${table.selectDynamic("serial")} = $serial"
      frag.query[Disk].run()
  extension [EC <: ItsaIDRepo](r: EC)
    def findAllByItsaId(id: String)(using DbCon)(using table: TableInfo[EC, ItsaEntityType[EC], Long])(using reader: DbCodec[ItsaEntityType[EC]]): Seq[ItsaEntityType[EC]] =
      val frag = sql"select * from $table where ${table.selectDynamic("itsaid")} = $id"
      frag.query[ItsaEntityType[EC]].run()
//
//  extension(r: HLRepo[MemoryCreator, Memory])
//    def insertOrUpdate(creator: MemoryCreator)(using table: TableInfo[MemoryCreator, Memory, Long]: Unit =
//

