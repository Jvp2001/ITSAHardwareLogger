package org.itsadigitaltrust.hardwarelogger.services

import scala.compiletime.uninitialized


trait HardwareGrabberService:

  import org.itsadigitaltrust.hardwarelogger.models.*

  private var info: GeneralInfoModel = uninitialized
  private var drives: List[HardDriveModel] = List()
  private var mem: List[MemoryModel] = List()
  private var procs: List[ProcessorModel] = List()
  private var med: List[MediaModel] = List()

  final def generalInfo: GeneralInfoModel = info

  protected final def generalInfo_=(newInfo: GeneralInfoModel): Unit = info = newInfo

  final def hardDrives: Seq[HardDriveModel] = drives

  protected final def hardDrives_=(newValue: Seq[HardDriveModel]): Unit = drives = newValue.toList

  final def memory: Seq[MemoryModel] = mem

  protected final def memory_=(newValue: Seq[MemoryModel]): Unit = mem = newValue.toList

  final def processors: Seq[ProcessorModel] = procs

  protected final def processors_=(newValue: Seq[ProcessorModel]): Unit = procs = newValue.toList

  final def media: Seq[MediaModel] = med

  protected final def media_=(newValue: Seq[MediaModel]): Unit = med = newValue.toList


  def load(): Unit

  protected def loadGeneralInfo(): Unit

  protected def loadHardDrives(): Unit

  protected def loadMemory(): Unit

  protected def loadProcessors(): Unit

  protected def loadMedia(): Unit
  
