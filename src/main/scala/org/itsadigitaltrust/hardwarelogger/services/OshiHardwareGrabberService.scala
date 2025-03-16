package org.itsadigitaltrust.hardwarelogger.services

import org.itsadigitaltrust.common.Maths.*
import org.itsadigitaltrust.hardwarelogger.models.*

import oshi.SystemInfo

import scala.jdk.CollectionConverters.*


class OshiHardwareGrabberService extends HardwareGrabberService:
  import org.itsadigitaltrust.hardwarelogger.models.HardDriveType.*
  private val systemInfo = new SystemInfo
  private val hal = systemInfo.getHardware
  override def getGeneralInfo: GeneralInfo =
    val serialNumber = hal.getComputerSystem.getSerialNumber
    val model = hal.getComputerSystem.getModel
    val vendor = hal.getComputerSystem.getManufacturer
    val os = System.getProperty("os.name")
    GeneralInfo("Need to add", "Need to add", model, vendor, serialNumber, os)

  //TODO: Implement
  override def getHardDrives: List[HardDrive] =
    hal.getDiskStores.asScala.map: disk =>
      val name = disk.getName
      val size = disk.getSize
      val model = disk.getModel
      HardDrive(100, size.GiB, model, disk.getSerial, NVME, isSSD = true)
    .toList

  override def getMemory: List[Memory] =
    hal.getMemory.getPhysicalMemory.asScala.map: memory =>
      val size = memory.getCapacity.MiB
      val description = memory.getManufacturer
      Memory(size, description)
    .toList



  override def getProcessors: List[Processor] =

    val pi = hal.getProcessor.getProcessorIdentifier
    val name = pi.getName
    val desc = pi.getFamily
    val width = if pi.isCpu64bit then 64 else 32
    val longDesc = pi.getVendor
    val cores = hal.getProcessor.getPhysicalProcessorCount
    val freq = hal.getProcessor.getCurrentFreq
    List(Processor(hal.getProcessor.getProcessorIdentifier.getModel, freq.sum / freq.length, desc, longDesc, pi.getProcessorID, width, cores))





  override def getMedia: List[Media] =
    List()
  
