package org.itsadigitaltrust.hardwarelogger.services


import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.models.HLModel.*

import scala.compiletime.uninitialized
import scala.util.NotGiven

trait HardwareGrabberService extends FrontendService:
  sealed trait Error extends Throwable
  final case class LoadError(message: String, cause: Throwable) extends Error

  object LoadError:
    def apply(message: String): LoadError = new LoadError(message, null)
    def apply(cause: Throwable): LoadError = new LoadError(cause.getMessage, cause)
  
  final type Result[T] = Either[Error, Seq[T]]

  final class HardwareGrabberException(message: String) extends HLFrontendException(message)

  final def generalInfo: Result[GeneralInfoModel] = loadGeneralInfo()
  final def hardDrives: Result[HardDriveModel] = loadHardDrives()
  final def memory: Result[MemoryModel] = loadMemory()
  final def processors: Result[ProcessorModel] = loadProcessors()
  final def media: Result[MediaModel] = loadMedia()
  
  def loadGeneralInfo(): Result[GeneralInfoModel]

  def loadHardDrives(): Result[HardDriveModel]

  def loadMemory(): Result[MemoryModel]

  def loadProcessors(): Result[ProcessorModel]

  def loadMedia(): Result[MediaModel]

end HardwareGrabberService
