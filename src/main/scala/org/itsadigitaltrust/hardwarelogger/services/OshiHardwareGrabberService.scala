package org.itsadigitaltrust.hardwarelogger.services

import org.itsadigitaltrust.common.Maths.*
import org.itsadigitaltrust.common.Types.percent
import org.itsadigitaltrust.hardwarelogger.models.*
import oshi.SystemInfo
import ox.{fork, supervised}

import scala.jdk.CollectionConverters.*


object OshiHardwareGrabberService extends HardwareGrabberService, ServicesModule:
  import org.itsadigitaltrust.hardwarelogger.models.HardDriveType.*
  private val systemInfo = new SystemInfo
  private val hal = systemInfo.getHardware

  override def load(): Unit =
    supervised:
      fork(loadGeneralInfo())
      fork(loadHardDrives())
      fork(loadMemory())
      fork(loadProcessors())
      fork(loadMedia())
    .join()
    notificationCentre.publish(NotificationChannel.Reload)

  override def loadGeneralInfo(): Unit =
    val serialNumber = hal.getComputerSystem.getSerialNumber
    val model = hal.getComputerSystem.getModel
    val vendor = hal.getComputerSystem.getManufacturer
    val os = System.getProperty("os.name")
    generalInfo = GeneralInfoModel("Need to add", "Need to add", model, vendor, serialNumber, os)

  //TODO: Implement
  override def loadHardDrives(): Unit =
    hardDrives = hal.getDiskStores.asScala.map: disk =>
      val name = disk.getName
      val size = disk.getSize
      val model = disk.getModel
      HardDriveModel(99.percent, 100.percent, size.GiB, model, disk.getSerial, NVME, isSSD = true)
    .toList


  override def loadMemory(): Unit =
    memory = hal.getMemory.getPhysicalMemory.asScala.map: memory =>
      val size = memory.getCapacity.MiB
      val description = memory.getManufacturer
      MemoryModel(size, description)
    .toList



  override def loadProcessors(): Unit =

    val pi = hal.getProcessor.getProcessorIdentifier
    val name = pi.getName
    val desc = pi.getFamily
    val width = if pi.isCpu64bit then 64 else 32
    val longDesc = pi.getVendor
    val cores = hal.getProcessor.getPhysicalProcessorCount
    val freq = hal.getProcessor.getCurrentFreq
    processors = List(ProcessorModel(name, freq.sum / freq.length, desc, longDesc, pi.getProcessorID, width, cores))





  override def loadMedia(): Unit =
    ()
  
