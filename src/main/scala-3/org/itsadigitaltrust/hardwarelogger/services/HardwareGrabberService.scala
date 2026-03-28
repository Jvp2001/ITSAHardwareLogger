package org.itsadigitaltrust.hardwarelogger.services


import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.models.*
import org.itsadigitaltrust.hardwarelogger.services.OshiHardwareGrabberApplicationService.{loadGeneralInfo, loadHardDrives, loadMedia, loadMemory, loadProcessors}
import org.itsadigitaltrust.hardwarelogger.tasks.{HardwareLoggerTaskContext, SimpleWaitHardwareLoggerGroupTaskGroupBuilder}
import org.itsadigitaltrust.hardwarelogger.tasks.HardwareLoggerTaskContext.Background

import sun.jvm.hotspot.runtime.PerfMemory.end

import scala.compiletime.uninitialized

trait HardwareGrabberService extends FrontendService:
  

  final class HardwareGrabberException(message: String) extends HLFrontendException(message)

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


  def load()(finished: () => Unit = () => ())(using HardwareLoggerTaskContext.WaitBackground): Unit =

    val taskGroupBuilder = SimpleWaitHardwareLoggerGroupTaskGroupBuilder[Unit]()

    ProgramMode.mode match
      case "Normal" =>
        taskGroupBuilder <<- Seq(loadGeneralInfo(),
          loadMemory(),
          loadProcessors(),
          loadMedia(),
          loadHardDrives()
        )

      case "HardDrive" =>
        taskGroupBuilder <-- loadHardDrives()
      case "Both" => ()
    end match

    val infoType = if ProgramMode.isInNormalMode then "Hardware" else "Hard Drive"
    taskGroupBuilder.run(s"Getting $infoType Information")(finished)
  end load


  protected def loadGeneralInfo()(using HardwareLoggerTaskContext.Background): Unit

  protected def loadHardDrives()(using HardwareLoggerTaskContext.Background): Unit

  protected def loadMemory()(using HardwareLoggerTaskContext.Background): Unit

  protected def loadProcessors()(using HardwareLoggerTaskContext.Background): Unit

  protected def loadMedia()(using HardwareLoggerTaskContext.Background): Unit
  
