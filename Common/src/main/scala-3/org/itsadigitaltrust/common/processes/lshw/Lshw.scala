package org.itsadigitaltrust.common.processes.lshw

import org.itsadigitaltrust.common.OSUtils
import org.itsadigitaltrust.common.Operators.|>
import org.itsadigitaltrust.common.processes.{ProcessConfig, sudo}
import org.itsadigitaltrust.common.processes.lshw.types.{CdRom, Device}

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.{JsonMappingException, RuntimeJsonMappingException}
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

private[processes] object Lshw:
  given jsonMapper: JsonMapper = JsonMapper.builder()
    .addModules(DefaultScalaModule())
    .build()




  def readDisks(`class`: String = "disk")(using ProcessConfig): Option[Seq[CdRom]] =
    val json = if OSUtils.onLinux then sudo"lshw -C ${`class`} -json" else "[\n{\n\"id\":\"cdrom\",\n\"class\":\"disk\",\n\"claimed\":true,\n\"handle\":\"SCSI:05:00:00:00\",\n\"description\":\"DVD-RAMwriter\",\n\"product\":\"DVDRWGUD1N\",\n\"vendor\":\"hpHLDS\",\n\"physid\":\"0.0.0\",\n\"businfo\":\"scsi@5:0.0.0\",\n\"logicalname\":[\"/dev/cdrom\",\"/dev/sr0\"],\n\"dev\":\"11:0\",\n\"version\":\"LD04\",\n\"configuration\":{\n\"ansiversion\":\"5\",\n\"status\":\"nodisc\"\n},\n\"capabilities\":{\n\"removable\":\"supportisremovable\",\n\"audio\":\"AudioCDplayback\",\n\"cd-r\":\"CD-Rburning\",\n\"cd-rw\":\"CD-RWburning\",\n\"dvd\":\"DVDplayback\",\n\"dvd-r\":\"DVD-Rburning\",\n\"dvd-ram\":\"DVD-RAMburning\"\n}\n},\n{\n\"id\":\"namespace:0\",\n\"class\":\"disk\",\n\"claimed\":true,\n\"description\":\"NVMedisk\",\n\"physid\":\"0\",\n\"logicalname\":\"hwmon0\"\n},\n{\n\"id\":\"namespace:1\",\n\"class\":\"disk\",\n\"claimed\":true,\n\"description\":\"NVMedisk\",\n\"physid\":\"2\",\n\"logicalname\":\"/dev/ng0n1\"\n},\n{\n\"id\":\"namespace:2\",\n\"class\":\"disk\",\n\"claimed\":true,\n\"handle\":\"GUID:1e990c7f-f492-4f38-ae82-4a2a73d7082b\",\n\"description\":\"NVMedisk\",\n\"physid\":\"1\",\n\"businfo\":\"nvme@0:1\",\n\"logicalname\":\"/dev/nvme0n1\",\n\"units\":\"bytes\",\n\"size\":256060514304,\n\"configuration\":{\n\"guid\":\"1e990c7f-f492-4f38-ae82-4a2a73d7082b\",\n\"logicalsectorsize\":\"512\",\n\"sectorsize\":\"512\",\n\"wwid\":\"eui.002538b371b386b8\"\n},\n\"capabilities\":{\n\"gpt-1.00\":\"GUIDPartitionTableversion1.00\",\n\"partitioned\":\"Partitioneddisk\",\n\"partitioned:gpt\":\"GUIDpartitiontable\"\n}\n}\n]"
    if json == "" then
      None
    else
      try
        val devices = jsonMapper.readValue(json, new TypeReference[Seq[Device]]
        {})
        devices.collect:
          case cdRom: CdRom => cdRom
        |> Option[Seq[CdRom]]

      catch
        case e: JsonMappingException =>
          System.err.println(s"CD deserialisation error: ${e.getMessage}")
          None
        case _ => None
      end try
  end readDisks

end Lshw








