package org.itsadigitaltrust.hdsentinelreader

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.`type`.TypeFactory
import com.fasterxml.jackson.dataformat.xml.{JacksonXmlModule, XmlMapper}
import com.fasterxml.jackson.module.jaxb.{JaxbAnnotationIntrospector, JaxbAnnotationModule}
import org.itsadigitaltrust.hdsentinelreader
import org.itsadigitaltrust.hdsentinelreader.data.HardDiskSummary

import java.io.File
import scala.io.Source
import scala.util.Using
import scala.xml.Document


object HDSentinelReader:

  import scala.compiletime.constValue





  private lazy val xmlMapper: XmlMapper =
    val mapper = new XmlMapper()
    mapper.registerModules(new JacksonXmlModule(), new JaxbAnnotationModule(new JaxbAnnotationIntrospector(TypeFactory.defaultInstance())))
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    mapper

  inline def apply(sudoPassword: String, inline outputFileName: XMLFile): Any =
    if System.getProperty("os.name").toLowerCase.contains("linux") then
      decode(ProcessRunner(sudoPassword, outputFileName))
    else
      //      Using(Source.fromFile(new File(outputFileName.toString))): source =>
      //
      //        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(source.getLines.mkString)
      //        document.getDocumentElement.normalize()
      //        println(document)
      //        val xmlMapper = createMapper
      //        val physicalDiskInformationDisk = xmlMapper.readValue(document.getElementById("Physical_Disk_Information_Disk_0").toString, classOf[PhysicalDiskInformationDisk0])
      //        println(physicalDiskInformationDisk)
      //        physicalDiskInformationDisk


      val xml = <HDSentinel>
        <Hard_Disk_Summary>
          <Hard_Disk_Number>0</Hard_Disk_Number>
          <Interface>S-ATA Gen3, 6 Gbps</Interface>
          <Disk_Controller>����������� ���������� Disk controller</Disk_Controller>
          <Disk_Location>Channel 1, Target 0, Lun 0, Device: 0</Disk_Location>
          <Hard_Disk_Model_ID>SSD Model</Hard_Disk_Model_ID>
          <Firmware_Revision>1234567890</Firmware_Revision>
          <Hard_Disk_Serial_Number>1234567890</Hard_Disk_Serial_Number>
          <SSD_Controller>SSD Controller</SSD_Controller>
          <Total_Size>12345 MB</Total_Size>
          <Power_State>Active</Power_State>
          <Logical_Drive_s>C: [VOLUME1] D: [VOLUME2]</Logical_Drive_s>
          <Current_Temperature>38 �C</Current_Temperature>
          <Maximum_Temperature_ever_measured>49 �C, 17.07.1970 22:28:37</Maximum_Temperature_ever_measured>
          <Minimum_Temperature_ever_measured>16 �C, 17.11.1970 14:42:12</Minimum_Temperature_ever_measured>
          <Daily_Average>38.80 �C</Daily_Average>
          <Daily_Maximum>40 �C</Daily_Maximum>
          <Power_on_time>1593 days, 6 hours</Power_on_time>
          <Estimated_remaining_lifetime>200 days</Estimated_remaining_lifetime>
          <Health>93 %</Health>
          <Performance>100 %</Performance>
          <Description>The status of the solid state disk is PERFECT. Problematic or weak sectors were not found. The TRIM feature of the SSD is supported and enabled for optimal performance. The health is determined by SSD specific S.M.A.R.T. attribute(s): #177 Wear Leveling Count</Description>
          <Tip>No actions needed.</Tip>
        </Hard_Disk_Summary>
      </HDSentinel>
      val document: Document = Document()
      val xmlString = Using(Source.fromFile(new File(outputFileName.toString)))(_.getLines.mkString).get

      val hardDiskSummaryElem = xml \ "Hard_Disk_Summary"
      val hardDiskSummary = xmlMapper.readValue(hardDiskSummaryElem.toString, classOf[HardDiskSummary])
      println(hardDiskSummary)


  def decode(xmlData: String): Any =
    //    val xml =
    //      """
    //        |<?xml version="1.0" encoding="ISO-8859-2"?>
    //        |  <Hard_Disk_Sentinel>
    //        |  <General_Information>
    //        |  <Application_Information>
    //        |  <Installed_version>Hard Disk Sentinel 0.20c-x64</Installed_version>
    //        |  <Current_Date_And_Time>30-3-25 16:56:56</Current_Date_And_Time>
    //        |  <Report_Creation_Time>0.124 s</Report_Creation_Time>
    //        |  </Application_Information>
    //        |  <Computer_Information>
    //        |  <Computer_Name>joshua-VMware-Virtual-Platform</Computer_Name>
    //        |  <MAC_Address>00:0c:29:5e:fe:ee</MAC_Address>
    //        |  </Computer_Information>
    //        |  <System_Information>
    //        |  <OS_Version>Linux : 6.11.0-17-generic (#17~24.04.2-Ubuntu SMP PREEMPT_DYNAMIC Mon Jan 20 22:48:29 UTC 2)</OS_Version>
    //        |  <Process_ID>7858</Process_ID>
    //        |  <Uptime>1212 sec (0 days, 0 hours, 20 min, 12 sec)</Uptime>
    //        |  </System_Information>
    //        |  </General_Information>
    //        |  <Physical_Disk_Information_Disk_0>
    //        |  <Hard_Disk_Summary>
    //        |  <Hard_Disk_Number>0</Hard_Disk_Number>
    //        |  <Hard_Disk_Device>/dev/sda</Hard_Disk_Device>
    //        |  <Interface>SCSI</Interface>
    //        |  <Hard_Disk_Model_ID>VMware, VMware Virtual S</Hard_Disk_Model_ID>
    //        |  <Firmware_Revision>1.0</Firmware_Revision>
    //        |  <Hard_Disk_Serial_Number>?</Hard_Disk_Serial_Number>
    //        |  <Total_Size>65535 MB</Total_Size>
    //        |  <Current_Temperature>?</Current_Temperature>
    //        |  <Maximum_temperature_during_entire_lifespan>?</Maximum_temperature_during_entire_lifespan>
    //        |  <Health>? %</Health>
    //        |  <Performance>? %</Performance>
    //        |  </Hard_Disk_Summary>
    //        |  <Properties>
    //        |  <Vendor_Information>?</Vendor_Information>
    //        |  <Status>OK</Status>
    //        |  <Version>2</Version>
    //        |  <Device_Type>Disk</Device_Type>
    //        |  <ASC>0</ASC>
    //        |  <ASCQ>0</ASCQ>
    //        |  <Bytes_Per_Sector>512</Bytes_Per_Sector>
    //        |  <Total_Sectors>134,217,727</Total_Sectors>
    //        |  <Unformatted_Capacity>68,719,476,224</Unformatted_Capacity>
    //        |  </Properties>
    //        |  <SCSI_Information>
    //        |  <Removable>Not supported [0]</Removable>
    //        |  <Failure_Prediction>Not supported [0]</Failure_Prediction>
    //        |  <Failure_Prediction>Disabled</Failure_Prediction>
    //        |  </SCSI_Information>
    //        |  </Physical_Disk_Information_Disk_0>
    //        |  <Partition_Information>
    //        |  <Partition Drive="/" Total_Space="64,197 MB" Free_Space="48,095 MB" Free_Space_Percent=" 75 %" Disk="/" BlockSize="4096" Files="4194304" FileSystem="61267" />
    //        |  <Partition Drive="/ (Disk #0)" Total_Space="64,197 MB" Free_Space="48,095 MB" Free_Space_Percent=" 75 %" Disk="/dev/sda2" BlockSize="4096" Files="4194304" FileSystem="61267" />
    //        |  </Partition_Information>
    //        |  </Hard_Disk_Sentinel>
    //        |""".stripMargin
    //


    //    val xmlData = """<?xml version="1.0" encoding="ISO-8859-2"?>
    //                  |<Hard_Disk_Sentinel>
    //                  |<General_Information>
    //                  |<Application_Information>
    //                  |<Installed_version>Hard Disk Sentinel 0.20c-x64</Installed_version>
    //                  |<Current_Date_And_Time>30-3-25 16:56:56</Current_Date_And_Time>
    //                  |<Report_Creation_Time>0.124 s</Report_Creation_Time>
    //                  |</Application_Information>
    //                  |<Computer_Information>
    //                  |<Computer_Name>joshua-VMware-Virtual-Platform</Computer_Name>
    //                  |<MAC_Address>00:0c:29:5e:fe:ee</MAC_Address>
    //                  |</Computer_Information>
    //                  |<System_Information>
    //                  |<OS_Version>Linux : 6.11.0-17-generic (#17~24.04.2-Ubuntu SMP PREEMPT_DYNAMIC Mon Jan 20 22:48:29 UTC 2)</OS_Version>
    //                  |<Process_ID>7858</Process_ID>
    //                  |<Uptime>1212 sec (0 days, 0 hours, 20 min, 12 sec)</Uptime>
    //                  |</System_Information>
    //                  |</General_Information>
    //                  |</Hard_Disk_Sentinel>""".stripMargin
    val xml = xmlData.replaceAll("<Disk_Information></<Disk_Information>", "").replaceAll("<Disk_Information/>", "") //.replaceAll("S.M.A.R.T.", "SMART").replaceAll("_", "").replaceAll("1.5", "1_5").replaceAll("<Disk_Information></<Disk_Information>", "")
//    val hardDiskSentinel = xmlMapper.readValue(xml, classOf[HardDiskSentinel])
//    println(hardDiskSentinel)
    ()






