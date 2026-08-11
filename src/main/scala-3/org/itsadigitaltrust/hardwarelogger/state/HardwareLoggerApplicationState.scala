package org.itsadigitaltrust.hardwarelogger.state

import org.itsadigitaltrust.common.Operators.{toOption, |>}
import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.common.{forkJoin, forkUserJoin, orThrowOption}
import org.itsadigitaltrust.hardwarelogger.models.HLModel.*
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.NotificationCentre
import org.itsadigitaltrust.hardwarelogger.services.{HLDatabaseService, HardwareGrabberService}
import org.itsadigitaltrust.hardwarelogger.tasks.HardwareLoggerSimpleTask
import org.itsadigitaltrust.hardwarelogger.views.Reloadable
import org.scalafx.extras
import org.scalafx.extras.BusyWorker.SimpleTask
import ox.Ox
import ox.either.{catching, fail, ok, orThrow}

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
  def reload[T <: SimpleTask[R], R](task: T, currentIndex: Double = 0D)(using hardwareGrabberService: HardwareGrabberService, databaseService: HLDatabaseService, totalNumberOfItems: Int = 0)(using Ox): Either[hardwareGrabberService.type#Error, Double] =
    val mode = ox.EitherMode[hardwareGrabberService.type#Error]()
    ox.supervisedError(mode):
      var index = currentIndex
      index += 1
      task.progress() = index /  totalNumberOfItems  // TODO put setting itsa id here.
      def performAsTask[A](msg: String)(body: scala.util.boundary.Label[Either[hardwareGrabberService.type#Error, Unit]] ?=> Either[hardwareGrabberService.type#Error, A]) =
        ox.forkUserError:
          ox.either:
            task.message() = msg

            body.map: res =>
              index += 1
              task.progress() = index / totalNumberOfItems
              res
        .join()
      end performAsTask

      performAsTask("Loading info..."):
        val option = hardwareGrabberService.loadGeneralInfo()
        data = data.copy(info = option.ok().toOption)
        option
      performAsTask("Loading memory..."):
        val value = hardwareGrabberService.loadMemory()
        data = data.copy(memory = value.ok().toOption)
        value

      performAsTask("Loading drives..."):
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
