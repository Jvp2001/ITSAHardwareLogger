package org.itsadigitaltrust.hardwarelogger.state

import org.itsadigitaltrust.common.Operators.{toOption, |>}
import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.common.{forkJoin, forkUserJoin, orThrowOption}
import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.models.HLModel.*
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.NotificationCentre
import org.itsadigitaltrust.hardwarelogger.services.{HLDatabaseService, HLDatabaseServiceException, HardwareGrabberService}
import org.itsadigitaltrust.hardwarelogger.tasks.HardwareLoggerSimpleTask
import org.itsadigitaltrust.hardwarelogger.views.Reloadable
import org.scalafx.extras
import org.scalafx.extras.BusyWorker.SimpleTask
import ox.Ox
import ox.either.{catching, fail, ok, orThrow}
import scalafx.application.Platform

import scala.::
import scala.languageFeature.implicitConversions
import scala.Conversion.into
import scala.concurrent.duration.DurationInt
import scala.math.Fractional.Implicits.infixFractionalOps
import scala.math.Integral.Implicits.infixIntegralOps

private[state] final case class Data(
                                      itsaID: Option[String] = None,
                                      info: Option[Seq[GeneralInfoModel]] = None,
                                      memory: Option[Seq[MemoryModel]] = None,
                                      drives: Option[Seq[HardDriveModel]] = None,
                                      media: Option[Seq[MediaModel]] = None,
                                      processors: Option[Seq[ProcessorModel]] = None
                                    )

case class HardwareLoggerApplicationState() extends HWLLoggable:
  private var data: Data = Data()

  def generalInfo: Option[Seq[GeneralInfoModel]] = data.info

  def memory: Option[Seq[MemoryModel]] =
    data.memory

  def hardDrives: Option[Seq[HardDriveModel]] = data.drives

  def media: Option[Seq[MediaModel]] =
    data.media

  def processors: Option[Seq[ProcessorModel]] = data.processors

  def itsaID: Option[String] = data.itsaID

  def itsaID_=(id: into[Option[String]]): Unit = data = data.copy(itsaID = id)

  def reload()(using hardwareGrabberService: HardwareGrabberService, databaseService: HLDatabaseService): Either[HardwareGrabberService#Error, Unit] =
    given [A]: Conversion[A, Option[A]] = Option.apply
    ox.either[HardwareGrabberService#Error, Data]:
      data = data.copy(
        info = hardwareGrabberService.loadGeneralInfo().ok(),
        memory = hardwareGrabberService.loadMemory().ok(),
        drives = hardwareGrabberService.loadHardDrives().ok(),
        media = hardwareGrabberService.loadMedia().ok(),
        processors = hardwareGrabberService.loadProcessors().ok()
      )
      data

    .map: result =>
      ()



    //    val itsaID = generalInfo.flatMap(_.headOption.flatMap(_.serial |> databaseService.findItsaIdBySerialNumber)).flatMap(_.orThrowOption)


  end reload

  /**
   *
   * @param task
   * @param hardwareGrabberService
   * @param databaseService
   * @param totalNumberOfItems
   * @param currentIndex
   * @param x$6
   * @tparam T
   * @tparam R
   * @return The new index for the current part of the task it is at.
   */
  def reload[T <: SimpleTask[R], R](task: T, currentIndex: Double = 0D)(using hardwareGrabberService: HardwareGrabberService, databaseService: HLDatabaseService, totalNumberOfItems: Int = 0)(using Ox): Either[Throwable, Double] =
    val mode = ox.EitherMode[Throwable]()
    ox.supervisedError(mode):
      var index = currentIndex
      def performAsTask(msg: String)(using allowedMode: ProgramMode = "Normal")(body: scala.util.boundary.Label[Either[Throwable, Unit]] ?=> Either[Throwable, Any]) =
        ox.forkUserError:
          ox.either:
            if ProgramMode.mode != allowedMode && allowedMode != "Both" then
              ()
            else
              Platform.runLater:
                task.message() = msg

              body.map: res =>
                index += 1

                Platform.runLater(task.progress() = index / totalNumberOfItems)
                res
        .join()
      end performAsTask


      performAsTask("Loading info...")(using allowedMode = "Both"):
        val option = hardwareGrabberService.loadGeneralInfo()
        data = data.copy(info = option.ok().toOption)
        option

      performAsTask("Checking for ITSA ID..."):
        val id = databaseService.findItsaIdBySerialNumber(data.info.get.head.serial)
        data = data.copy(itsaID = id.ok().toOption)
        if ProgramMode.mode == "Normal" then
          val newInfo = data.info.get.map: info =>
            info.copy(itsaID = id.ok().toOption)
          data = data.copy(info = newInfo.toOption)
        id


      performAsTask("Loading memory..."):
        val value = hardwareGrabberService.loadMemory()
        data = data.copy(memory = value.ok().toOption)
        value

      performAsTask("Loading drives...")(using allowedMode = "Both"):
        val value = hardwareGrabberService.loadHardDrives()
        data = data.copy(drives = value.ok().toOption)
        value

      performAsTask("Loading processors..."):
        val processors = hardwareGrabberService.loadProcessors()
        logger.info(s"Loaded processors $processors")
        data = data.copy(processors = processors.ok().toOption)
        processors

      performAsTask("Loading media..."):
        val media = hardwareGrabberService.loadMedia()
        data = data.copy(media = media.ok().toOption)
        media


      Right(index)
  end reload


end HardwareLoggerApplicationState
