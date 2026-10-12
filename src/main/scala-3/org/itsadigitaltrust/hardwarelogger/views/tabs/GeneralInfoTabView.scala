package org.itsadigitaltrust.hardwarelogger.views.tabs

import org.itsadigitaltrust.common.Operators.{??, |>, ||>}
import org.itsadigitaltrust.hardwarelogger.core.BuildInfo
import org.itsadigitaltrust.hardwarelogger.models.*
import org.itsadigitaltrust.hardwarelogger.viewmodels.tabs.{GeneralInfoTabViewModel, TabTableViewModel}
import org.itsadigitaltrust.hardwarelogger.services.HardwareGrabberService
import org.itsadigitaltrust.hardwarelogger.services.database.HLDatabaseService
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState
import scalafx.beans.property.StringProperty
import org.itsadigitaltrust.hardwarelogger.core.*
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{NotificationCentre, ReloadNotificationListener}
import org.itsadigitaltrust.hardwarelogger.ui.derivation.annotations.{DisplayName, Readonly, UISchemaAnnotation}
import org.itsadigitaltrust.hardwarelogger.ui.derivation.{DeferredField, FieldUI, UIControlFactory, UIGeneration}
import org.itsadigitaltrust.hardwarelogger.views.View
import scalafx.application.Platform
import scalafx.scene.layout.ColumnConstraints
import ui.*

import scala.jdk.javaapi.CollectionConverters.asJavaCollection
import scala.reflect.classTag
import scala.language.experimental.modularity

private[views] final class GeneralInfoTabView(using GeneralInfoTabViewModel) extends VBox(5), ReloadNotificationListener, View[GeneralInfoTabViewModel]:
  import  GeneralInfoTabView.given 
  import org.itsadigitaltrust.hardwarelogger.ui.ui.given

  reload()
  override def reload(): Either[Error, Unit] =
    Platform.runLater:
      viewModel.info.foreach: info =>
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
    private def driveStatusColour(status: GeneralDriveStatus): String =
      status match
        case GeneralDriveStatus.BadHealth => "-color-danger-fg"
        case GeneralDriveStatus.NotLogged => "-color-warning-fg"
        case GeneralDriveStatus.NoIssues => "-color-success-fg"

    private def driveStatusLabel(status: GeneralDriveStatus): Label =
      new Label(status.toString):
        delegate.setStyle(s"-fx-text-fill: ${driveStatusColour(status)};")

    // Manual given definition overrides automatic macro structural mirror constraints cleanly
    given UIControlFactory[Seq[GeneralDriveStatus]] with

      override def createField(initialValue: Seq[GeneralDriveStatus], annotations: List[UISchemaAnnotation]): DeferredField[Seq[GeneralDriveStatus]] = 
        DeferredField(
          controlBuilder = () =>
           if initialValue.length == 1 && initialValue.head == GeneralDriveStatus.NoIssues then
             driveStatusLabel(initialValue.head)
           else
             new VBox:
               children ++= initialValue.map: status =>
                 driveStatusLabel(status)
          ,
          flushValue = () => ObservableBuffer(initialValue*).toSeq
        )
