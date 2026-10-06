package org.itsadigitaltrust.hardwarelogger.views

import javafx.scene.control.{SplitPane, TextFormatter}
import javafx.util.converter.DefaultStringConverter
import org.itsadigitaltrust.common.Operators.{?, ??, or, |>}
import scalafx.beans.property.StringProperty
import org.itsadigitaltrust.hardwarelogger.core.ui.*
import scalafx.Includes.*
import org.itsadigitaltrust.hardwarelogger.issuereporter.{Description, ReportedIssue}
import org.itsadigitaltrust.hardwarelogger.services.{IssueReporterService, ReportedIssue}
import ch.qos.logback.classic.LoggerContext
import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.hardwarelogger.core.HWLoggerShowMessage
import org.itsadigitaltrust.logviewer.{ItsaLogViewer, LogIconFactory}
import org.kordamp.ikonli.javafx.FontIcon
import org.kordamp.ikonli.materialdesign2.MaterialDesignA
import org.kordamp.ikonli.materialdesign2.MaterialDesignB
import org.kordamp.ikonli.materialdesign2.MaterialDesignI
import org.kordamp.ikonli.materialdesign2.MaterialDesignM
import org.scalafx.extras.auto_dialog.AutoDialog
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import qupath.ui.logviewer.api.manager.LoggerManager
import qupath.ui.logviewer.ui.main.LogViewer
import scalafx.application.Platform
import scalafx.scene.control.Alert.AlertType.{Confirmation, Information}
import scalafx.scene.control.{CheckMenuItem, ContextMenu, Dialog, MenuItem, SeparatorMenuItem, TextArea}
import scalafx.scene.input.{Clipboard, ClipboardContent}
import scalafx.util.StringConverter

private given logIconFactory: LogIconFactory with
  def createIcon(level: Level): Option[Node] =
    val icon = new FontIcon()
    level match
      case Level.TRACE =>
        icon.setIconCode(MaterialDesignM.MAGNIFY)
        icon.getStyleClass.add("text-subtle")
      case Level.DEBUG =>
        icon.setIconCode(MaterialDesignB.BUG)
        icon.getStyleClass.add("accent")
      case Level.INFO =>
        icon.setIconCode(MaterialDesignI.INFORMATION)
        icon.getStyleClass.add("text")
      case Level.WARN =>
        icon.setIconCode(MaterialDesignA.ALERT)
        icon.getStyleClass.add("warning")
      case Level.ERROR =>
        icon.setIconCode(MaterialDesignA.ALERT_OCTAGON)
        icon.getStyleClass.add("danger")
  
    icon.setIconSize(14)
    icon.?
end logIconFactory

class ItsaDebugView(using LoggerManager) extends ItsaLogViewer with HWLLoggable:
    vgrow = Always
    stylesheets += stylesheet"logviewer.css"
  
    Platform.runLater:
      logger.info("ItsaDebugView created")
    override def toStyleClasses(level: Level): List[String] =
      val commonStyleClasses = List("text")
      level match
        case Level.ERROR => "danger" :: commonStyleClasses
        case Level.WARN => "warning" :: commonStyleClasses
        case _ => commonStyleClasses


object ItsaDebugView:

  import org.itsadigitaltrust.hardwarelogger.services.ReportedIssue as Issue
  import org.itsadigitaltrust.hardwarelogger.core.given

 

//
//  def report(reporter: Issue => Option[String])(using debugView: ItsaDebugView, showMessage: HWLoggerShowMessage): Unit =
//    def sendReport(issue: Issue = Issue()) =
//      reporter(makeReport(issue)) match
//        case Some(value) => showMessage.showError("Issue Submission failed", value)
//        case None => showMessage.showInformation("Issue Reported!")
//
//
//    def makeReport(issue: Issue = Issue()): Issue =
//      val title: String = issue.title
//      val description = issue.description.value ?? "" +
//        s"""
//           |
//           |Output
//           |==========================================
//           |TODO
//           |""".stripMargin
//      Issue(title, Description(description))
//    end makeReport
//
//
//
//    showMessage.showConfirmation("Report Issue", "Do you want to report an Issue").match
//      case Some(ButtonType.No) => ()
//      case Some(ButtonType.Yes | ButtonType.OK) =>
//        showMessage.showConfirmation("Customise Issue", "Do you want to customise the issue?").match
//          case Some(ButtonType.No) =>  sendReport()
//          case Some(ButtonType.Yes | ButtonType.OK) =>
//            val dialog = new Dialogs.IssueCustomisationDialog()
//            dialog.showDialog()
//            if dialog.wasOKed then
//              sendReport(dialog.getIssue)
//            ()
//          case _ => ()
//      case _ => ()


end ItsaDebugView


