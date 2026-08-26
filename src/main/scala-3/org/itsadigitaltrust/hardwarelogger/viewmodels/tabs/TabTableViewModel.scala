package org.itsadigitaltrust.hardwarelogger.viewmodels.tabs

import com.somainer.nameof.NameOf
import org.itsadigitaltrust.common.Operators.{??, toOption, |>}
import org.itsadigitaltrust.hardwarelogger.HardwareLoggerApplication.databaseService
import org.itsadigitaltrust.hardwarelogger.delegates.{ProgramMode, TableRowDelegate}
import org.itsadigitaltrust.hardwarelogger.models.HLModel
import org.itsadigitaltrust.hardwarelogger.services.{ApplicationServicesRegistry, HardwareGrabberService}
import org.itsadigitaltrust.hardwarelogger.viewmodels.ViewModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.TableRowViewModel
import scalafx.beans.property.BooleanProperty
import scalafx.collections.*
import scalafx.scene.control.TableView
import scalafx.scene.input.KeyCode
import org.itsadigitaltrust.hardwarelogger.core.ui.*
import org.itsadigitaltrust.hardwarelogger.models
import org.itsadigitaltrust.hardwarelogger.services.OshiHardwareGrabberApplicationService.LoadError
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{PerformBackendValidationListener, RequiresGlobalValidationContext, RequiresItsaID}
import org.itsadigitaltrust.hardwarelogger.services.validation.GlobalValidationContext
import org.itsadigitaltrust.hardwarelogger.shared.HardwareLoggerSharedData
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState
import org.scalafx.extras
import ox.{Fork, Ox}
import ox.either.{fail, ok}
import scalafx.application.Platform

import scala.reflect.{ClassTag, classTag}


class TabTableViewModel[M <: HLModel : ClassTag, VM <: TableRowViewModel[M]]
(using hardwareGrabberService: HardwareGrabberService, hardwareLoggerApplicationState: HardwareLoggerApplicationState)
(rowCtor: M => VM, reloadData: HardwareLoggerSharedData => Option[Seq[M]], saveDataMode: ProgramMode = "Normal")
  extends ViewModel, RequiresItsaID, RequiresGlobalValidationContext, PerformBackendValidationListener:

  type RowViewModel = VM
  val data: ObservableBuffer[VM] = ObservableBuffer()



  override protected def onBackendValidationFork(): Unit =
    ox.supervised:
      databaseService.runValidation(data.map(_.model).toSeq*)


  protected override def reload()(using HardwareLoggerSharedData): Either[Error, Unit] =
    import TabTableViewModel.toLoadEitherError
    ox.either:
      logger.info(s"Reloading ${classTag[M].runtimeClass.getSimpleName} data")
      val result = reloadData(summon[HardwareLoggerSharedData]).toLoadEitherError.ok()

      data.setAll(result.map(rowCtor) *)
      logger.info(s"Added ${data.size} item(s).")



  //  data.onChange: (source, change) =>
  //    if change.isEmpty then
  //      extras.offFX:
  //        ox.supervised:
  //          reload()


  def add(model: M): this.type =
    data.add(rowCtor(model))
    this

  def models: Seq[M] = data.map(_.model).toSeq


  override def onProgramModeChanged(mode: ProgramMode): Either[Error, Unit] =
    ox.either:
      if saveDataMode == "Both" || saveDataMode == ProgramMode.mode then
        logger.info(s"Saving ${models.size} models")

      val editable: BooleanProperty = BooleanProperty(false)
      editable.bind(ProgramMode.isHardDriveMode)

  end onProgramModeChanged

  //  def models: Seq[M] = data.map(_.model).toSeq


  //      extras.onFX:


  override protected def save(sharedData: HardwareLoggerSharedData)(using Ox): Either[Error, Unit] =
    ox.fork:
      databaseService.executeFinalSave(data.map(_.model).toSeq*)
    .join()
    Right(())

end TabTableViewModel


object TabTableViewModel:
  extension [M <: HLModel : ClassTag](option: Option[Seq[M]])
    def toLoadEitherError: Either[LoadError, Seq[M]] =
      option.toRight(LoadError(s"Failed to load data of type ${classTag[M].runtimeClass.getSimpleName}"))