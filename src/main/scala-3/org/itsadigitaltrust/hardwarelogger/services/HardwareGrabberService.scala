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

  final def generalInfo(using ox.Ox): Result[GeneralInfoModel] = loadGeneralInfo()
  final def hardDrives(using ox.Ox): Result[HardDriveModel] = loadHardDrives()
  final def memory(using ox.Ox): Result[MemoryModel] = loadMemory()
  final def processors(using ox.Ox): Result[ProcessorModel] = loadProcessors()
  final def media(using ox.Ox): Result[MediaModel] = loadMedia()
  
  def loadGeneralInfo()(using ox.Ox): Result[GeneralInfoModel]

  def loadHardDrives()(using ox.Ox): Result[HardDriveModel]

  def loadMemory()(using ox.Ox): Result[MemoryModel]

  def loadProcessors()(using ox.Ox): Result[ProcessorModel]

  def loadMedia()(using ox.Ox): Result[MediaModel]

end HardwareGrabberService
