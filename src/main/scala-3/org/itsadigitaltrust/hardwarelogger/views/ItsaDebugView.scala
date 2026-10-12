package org.itsadigitaltrust.hardwarelogger.views

import javafx.scene.control.{SplitPane, TextFormatter}
import javafx.util.converter.DefaultStringConverter
import org.itsadigitaltrust.common.Operators.{?, ??, or, |>}
import scalafx.beans.property.StringProperty
import org.itsadigitaltrust.hardwarelogger.core.ui.*
import org.itsadigitaltrust.hardwarelogger.services.{IssueReporterService, LoggerManagerModule}
import ch.qos.logback.classic.LoggerContext
import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.hardwarelogger.core.HWLoggerShowMessage
import org.itsadigitaltrust.logviewer.{ItsaLogViewer, LogIconFactory}
import org.scalafx.extras.auto_dialog.AutoDialog
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import qupath.ui.logviewer.api.manager.LoggerManager
import qupath.ui.logviewer.logging.logback.LogbackManager
import qupath.ui.logviewer.ui.main.LogViewer
import scalafx.application.Platform
import scalafx.scene.control.Alert.AlertType.{Confirmation, Information}
import scalafx.scene.control.{CheckMenuItem, ContextMenu, Dialog, MenuItem, SeparatorMenuItem, TextArea}
import scalafx.scene.input.{Clipboard, ClipboardContent}
import scalafx.util.StringConverter

private given logIconFactory: LogIconFactory with
  def createIcon(level: Level): Option[Node] =
    val (symbol, styleClass) = level match
      case Level.TRACE =>
        ("T", "text-subtle")
      case Level.DEBUG =>
        ("D", "accent")
      case Level.INFO =>
        ("I", "text")
      case Level.WARN =>
        ("!", "warning")
      case Level.ERROR =>
        ("X", "danger")

    val icon = new Label(symbol)
    icon.setMinWidth(18)
    icon.setAlignment(Pos.Center)
    icon.getStyleClass.addAll("log-level", styleClass)
    Some(icon)
end logIconFactory

// TODO: Pass this in.
class ItsaDebugView/*(using LoggerManager)*/ extends ItsaLogViewer(using new LogbackManager) with HWLLoggable:
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

