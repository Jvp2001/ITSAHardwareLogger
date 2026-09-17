package org.itsadigitaltrust.common.processes.lshw

import org.itsadigitaltrust.common.OSUtils
import org.itsadigitaltrust.common.Operators.|>
import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.common.processes.{SudoError, sudo}
import org.itsadigitaltrust.common.processes.lshw.types.{CdRom, Device}
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.{JsonMappingException, ObjectMapper, RuntimeJsonMappingException}
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import ox.either.{fail, ok}

object Lshw extends HWLLoggable:
  private given jsonMapper: ObjectMapper =
    val om = new ObjectMapper
    om.registerModule(DefaultScalaModule)
    om


  enum LshwError:
    case NoMediaDrivesFound
    case DeserialisationError
    case UnknownError(e: Throwable)

    override def toString: String = 
      this match
        case NoMediaDrivesFound => "No media drives found"
        case DeserialisationError => "Deserialisation error"
        case UnknownError(e) => s"Unknown error: ${e.getMessage}"
        
  end LshwError
  
    

  def readDisks(`class`: String = "disk")(using testing: Boolean = false): Either[SudoError | LshwError, Seq[CdRom]] =
    try
      ox.either:
//      val json =
//        if OSUtils.onLinux then
//          sudo"lshw -C ${`class`} -json".ok()
//
//        else if testing then
//          "[\n{\n\"id\":\"cdrom\",\n\"class\":\"disk\",\n\"claimed\":true,\n\"handle\":\"SCSI:05:00:00:00\",\n\"description\":\"DVD-RAMwriter\",\n\"product\":\"DVDRWGUD1N\",\n\"vendor\":\"hpHLDS\",\n\"physid\":\"0.0.0\",\n\"businfo\":\"scsi@5:0.0.0\",\n\"logicalname\":[\"/dev/cdrom\",\"/dev/sr0\"],\n\"dev\":\"11:0\",\n\"version\":\"LD04\",\n\"configuration\":{\n\"ansiversion\":\"5\",\n\"status\":\"nodisc\"\n},\n\"capabilities\":{\n\"removable\":\"supportisremovable\",\n\"audio\":\"AudioCDplayback\",\n\"cd-r\":\"CD-Rburning\",\n\"cd-rw\":\"CD-RWburning\",\n\"dvd\":\"DVDplayback\",\n\"dvd-r\":\"DVD-Rburning\",\n\"dvd-ram\":\"DVD-RAMburning\"\n}\n},\n{\n\"id\":\"namespace:0\",\n\"class\":\"disk\",\n\"claimed\":true,\n\"description\":\"NVMedisk\",\n\"physid\":\"0\",\n\"logicalname\":\"hwmon0\"\n},\n{\n\"id\":\"namespace:1\",\n\"class\":\"disk\",\n\"claimed\":true,\n\"description\":\"NVMedisk\",\n\"physid\":\"2\",\n\"logicalname\":\"/dev/ng0n1\"\n},\n{\n\"id\":\"namespace:2\",\n\"class\":\"disk\",\n\"claimed\":true,\n\"handle\":\"GUID:1e990c7f-f492-4f38-ae82-4a2a73d7082b\",\n\"description\":\"NVMedisk\",\n\"physid\":\"1\",\n\"businfo\":\"nvme@0:1\",\n\"logicalname\":\"/dev/nvme0n1\",\n\"units\":\"bytes\",\n\"size\":256060514304,\n\"configuration\":{\n\"guid\":\"1e990c7f-f492-4f38-ae82-4a2a73d7082b\",\n\"logicalsectorsize\":\"512\",\n\"sectorsize\":\"512\",\n\"wwid\":\"eui.002538b371b386b8\"\n},\n\"capabilities\":{\n\"gpt-1.00\":\"GUIDPartitionTableversion1.00\",\n\"partitioned\":\"Partitioneddisk\",\n\"partitioned:gpt\":\"GUIDpartitiontable\"\n}\n}\n]"
//        else ""

        import sys.process.*
        if "sudo hwinfo --cdrom".!!.nonEmpty then

          val json = "sudo lshw -C ${`class`} -json".!!


            if json.nonEmpty then
              jsonMapper.readValue(json, classOf[Array[Device]])
                .collect:
                  case cdRom: CdRom => cdRom
              .toSeq
              |> Seq[CdRom]
            else
              Seq.empty[CdRom]
            end if
        else
          Seq.empty[CdRom]
        end if


    catch
      case e: JsonMappingException =>
        logger.error(s"CD deserialisation error: ${e.getMessage}")
        Left(LshwError.DeserialisationError)
      case e: Exception => Left(LshwError.UnknownError(e))
    end try
  end readDisks

end Lshw
