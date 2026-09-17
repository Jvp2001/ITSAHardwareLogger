package org.itsadigitaltrust.hardwarelogger.services


import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.models.HLModel.*

import scala.compiletime.uninitialized
import scala.util.NotGiven

trait HardwareGrabberService extends FrontendService:
  sealed trait Error extends Throwable
  final case class LoadError(message: String, cause: Throwable) extends Error
  final case class InvalidPermissionsError() extends Error
  
  object LoadError:
    def apply(message: String): LoadError = new LoadError(message, null)
    def apply(cause: Throwable): LoadError = new LoadError(cause.getMessage, cause)
  
  final type Result[T] = Either[Error, Seq[T]]

  final class HardwareGrabberException(message: String) extends HLFrontendException(message)
  
  
  def loadGeneralInfo()(using ox.Ox): Result[GeneralInfoModel]

  def loadHardDrives()(using ox.Ox): Result[HardDriveModel]

  def loadMemory()(using ox.Ox): Result[MemoryModel]

  def loadProcessors()(using ox.Ox): Result[ProcessorModel]

  def loadMedia()(using ox.Ox): Result[MediaModel]

  
  
  /**
   This is only an option because it may not be present on all systems
   */
  def loadBatteryStatus()(using ox.Ox): Option[BatteryStatusModel]
end HardwareGrabberService
