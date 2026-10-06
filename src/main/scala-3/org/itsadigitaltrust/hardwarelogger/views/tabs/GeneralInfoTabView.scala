package org.itsadigitaltrust.hardwarelogger.views.tabs

import org.itsadigitaltrust.common.Operators.{??, |>, ||>}
import org.itsadigitaltrust.hardwarelogger.core.BuildInfo
import org.itsadigitaltrust.hardwarelogger.models.*
import org.itsadigitaltrust.hardwarelogger.viewmodels.tabs.TabTableViewModel
import org.itsadigitaltrust.hardwarelogger.services.HardwareGrabberService
import org.itsadigitaltrust.hardwarelogger.services.database.HLDatabaseService
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState
import scalafx.beans.property.StringProperty
import org.itsadigitaltrust.hardwarelogger.core.*
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.NotificationCentre
import org.itsadigitaltrust.hardwarelogger.ui.derivation.annotations.{DisplayName, Readonly, UISchemaAnnotation}
import org.itsadigitaltrust.hardwarelogger.ui.derivation.{DeferredField, FieldUI, UIControlFactory, UIGeneration}
import scalafx.scene.layout.ColumnConstraints
import ui.*

import scala.jdk.javaapi.CollectionConverters.asJavaCollection
import scala.reflect.classTag
import scala.language.experimental.modularity

private[views] final class GeneralInfoTabView(using HardwareGrabberService, HLDatabaseService)(using applicationState: HardwareLoggerApplicationState) extends VBox(5):

  NotificationCentre.addReloadListener(): () =>
    reload()

  reload()




  //
//    private val computerIDColumn = createAndAddColumn("Version ID", minimumWidth = ColumnSize.big): cellValue =>
//      val string = cellValue.computerIDProperty.value + s" ${BuildInfo.version.replace("-SNAPSHOT", "")}"
//      string.replace("null ", "ITSAHardwareLogger ")
//      |> StringProperty.apply


  import  GeneralInfoTabView.given 
  import org.itsadigitaltrust.hardwarelogger.ui.ui.given
  private def reload() =
    applicationState.generalInfo.map(_.head).map(_.head).foreach: info =>
      children.clear()
      val pane = UIGeneration.createInfoPane(info)
      pane.columnConstraints = Seq(ColumnConstraints(), new ColumnConstraints():
        hgrow = Always
        margin = Insets(10, 0, 0, 10)
      )
      VBox.setVgrow(pane, Always)
      children += pane
    Right(())
end GeneralInfoTabView


object GeneralInfoTabView:
    // Manual given definition overrides automatic macro structural mirror constraints cleanly
    given UIControlFactory[Seq[GeneralDriveStatus]] with
      override def createField(initialValue: Seq[HLModel.GeneralDriveStatus], annotations: List[UISchemaAnnotation]): DeferredField =
        val isReadOnly = annotations.exists(_.isInstanceOf[Readonly])
        DeferredField(
          controlBuilder = () =>
           if initialValue.length == 1 && initialValue.head == GeneralDriveStatus.NoIssues then
             new Label:
               text = initialValue.head.toString
               style = s"-fx-text-fill: -color-success-fg;"
           else
             new VBox:
               children ++= initialValue.map: status =>
                 new Label:
                   text = status.toString
                   private val colour = status match
                     case GeneralDriveStatus.BadHealth => "-color-error-fg"
                     case GeneralDriveStatus.NotLogged => "-color-warning-fg"
                     case GeneralDriveStatus.NoIssues => "-color-success-fg"
                   style = s"-fx-text-fill: $colour;"
          ,
          flushValue = () => ObservableBuffer(initialValue*)
        )
