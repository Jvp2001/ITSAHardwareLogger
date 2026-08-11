package org.itsadigitaltrust.hardwarelogger.viewmodels.tabs

import com.somainer.nameof.NameOf
import org.itsadigitaltrust.common.Operators.{??, toOption, |>, ||>}
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
import org.itsadigitaltrust.hardwarelogger.services.OshiHardwareGrabberApplicationService.LoadError
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState
import org.scalafx.extras
import ox.either.{fail, ok}
import scalafx.application.Platform

import scala.reflect.{ClassTag, classTag}


class TabTableViewModel[M <: HLModel : ClassTag, VM <: TableRowViewModel[M]](using hardwareGrabberService: HardwareGrabberService, hardwareLoggerApplicationState: HardwareLoggerApplicationState)(rowCtor: M => VM, reloadData: HardwareLoggerApplicationState => Option[Seq[M]], saveDataMode: ProgramMode = "Normal") extends ViewModel:

  type RowViewModel = VM
  val data: ObservableBuffer[VM] = ObservableBuffer()
  reloadData(hardwareLoggerApplicationState).map: rdata =>
    rdata.map(rowCtor)
  .foreach(data.addAll)
//  data.onChange: (source, change) =>
//    if change.isEmpty then
//      extras.offFX:
//        ox.supervised:
//          reload()

  

  def add(model: M): this.type =
    data.add(rowCtor(model))
    this



  override def onProgramModeChanged(mode: ProgramMode): Either[Error, Unit] =
    ox.either:
      if saveDataMode == "Both" || saveDataMode == ProgramMode.mode then
        logger.info(s"Saving ${models.size} models")
      
      val editable: BooleanProperty = BooleanProperty(false)
      editable.bind(ProgramMode.isHardDriveMode)
    
  end onProgramModeChanged
  
  def models: Seq[M] = data.map(_.model).toSeq

  override def reload(shouldClearData: Boolean = true)(using applicationState: HardwareLoggerApplicationState): Either[Error, Unit] =
    import TabTableViewModel.toLoadEitherError
    ox.either:
      logger.info(s"Reloading ${classTag[M].runtimeClass.getSimpleName} data")
      val result = reloadData(applicationState).toLoadEitherError.ok()

      data.clear()
      data ++= result.map(rowCtor)
      logger.info(s"Added ${data.size} item(s).")



//      extras.onFX:


end TabTableViewModel



object TabTableViewModel:
  extension[M <: HLModel : ClassTag](option: Option[Seq[M]])
    def toLoadEitherError: Either[LoadError, Seq[M]] =
      option.toRight(LoadError(s"Failed to load data of type ${classTag[M].runtimeClass.getSimpleName}"))