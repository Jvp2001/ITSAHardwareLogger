package org.itsadigitaltrust.hardwarelogger.services

import org.itsadigitaltrust.common.Maths.*
import org.itsadigitaltrust.hardwarelogger.models.*

import oshi.SystemInfo

import scala.jdk.CollectionConverters.*


class OshiHardwareGrabberService extends HardwareGrabberService:
  import org.itsadigitaltrust.hardwarelogger.models.HardDriveType.*
  private val systemInfo = new SystemInfo
  private val hal = systemInfo.getHardware
  override def getGeneralInfo: GeneralInfoModel =
    val serialNumber = hal.getComputerSystem.getSerialNumber
    val model = hal.getComputerSystem.getModel
    val vendor = hal.getComputerSystem.getManufacturer
    val os = System.getProperty("os.name")
    GeneralInfoModel("Need to add", "Need to add", model, vendor, serialNumber, os)

  //TODO: Implement
  override def getHardDrives: List[HardDriveModel] =
    hal.getDiskStores.asScala.map: disk =>
      val name = disk.getName
      val size = disk.getSize
      val model = disk.getModel
      HardDriveModel(100, size.GiB, model, disk.getSerial, NVME, isSSD = true)
    .toList

  override def getMemory: List[MemoryModel] =
    hal.getMemory.getPhysicalMemory.asScala.map: memory =>
      val size = memory.getCapacity.MiB
      val description = memory.getManufacturer
      MemoryModel(size, description)
    .toList



  override def getProcessors: List[ProcessorModel] =

    val pi = hal.getProcessor.getProcessorIdentifier
    val name = pi.getName
    val desc = pi.getFamily
    val width = if pi.isCpu64bit then 64 else 32
    val longDesc = pi.getVendor
    val cores = hal.getProcessor.getPhysicalProcessorCount
    val freq = hal.getProcessor.getCurrentFreq
    List(ProcessorModel(hal.getProcessor.getProcessorIdentifier.getModel, freq.sum / freq.length, desc, longDesc, pi.getProcessorID, width, cores))





  override def getMedia: List[MediaModel] =
    List()
  
