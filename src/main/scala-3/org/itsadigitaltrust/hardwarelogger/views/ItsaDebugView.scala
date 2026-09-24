package org.itsadigitaltrust.hardwarelogger.views

import javafx.scene.control.{SplitPane, TextFormatter}
import javafx.util.converter.DefaultStringConverter
import org.itsadigitaltrust.common.Operators.{??, or, |>}
import scalafx.beans.property.StringProperty
import org.itsadigitaltrust.hardwarelogger.core.ui.*
import scalafx.Includes.*
import org.itsadigitaltrust.hardwarelogger.issuereporter.{Description, ReportedIssue}
import org.itsadigitaltrust.hardwarelogger.services.{IssueReporterService, ReportedIssue}
import ch.qos.logback.classic.LoggerContext
import org.itsadigitaltrust.hardwarelogger.core.HWLoggerShowMessage
import org.scalafx.extras.auto_dialog.AutoDialog
import org.slf4j.LoggerFactory
import qupath.ui.logviewer.ui.main.LogViewer
import scalafx.scene.control.Alert.AlertType.{Confirmation, Information}
import scalafx.scene.control.{CheckMenuItem, ContextMenu, Dialog, MenuItem, SeparatorMenuItem, TextArea}
import scalafx.scene.input.{Clipboard, ClipboardContent}
import scalafx.util.StringConverter

class ItsaDebugView(using tabPane: TabPane, issueReporterService: IssueReporterService) extends VBox:


  // 1. Instantiate your overridden log viewer component
  val logViewerInstance = new LogViewer()

  // 1. Force the custom view to automatically mimic its parent's width and height bounds
  logViewerInstance.prefWidthProperty().bind(tabPane.widthProperty())
  logViewerInstance.prefHeightProperty().bind(tabPane.heightProperty())


class ExtendedLogViewer(using issueReporterService: IssueReporterService) extends LogViewer:


  // 1. Force the overall LogViewer to grow inside your parent Tab/VBox
  VBox.setVgrow(this, Priority.Always)

  // 2. Find and fix the SplitPane internal layout stretching rules
  Option(this.getCenter) match
    case Some(splitPane: SplitPane) =>
      VBox.setVgrow(splitPane, Priority.Always)

      val items = splitPane.getItems
      if items.size() >= 2 then
        val tableContainer = items.get(0)
        val textContainer = items.get(1)

        // Tell the SplitPane to give all expanding room to the TableView
        SplitPane.setResizableWithParent(tableContainer, true)
        SplitPane.setResizableWithParent(textContainer, false)

        // Set the starting divider ratio split (75% Table, 25% Text Area)
        splitPane.setDividerPositions(0.75)

    case Some(otherContainer) =>
      VBox.setVgrow(otherContainer, Priority.Always)

    case None =>
// Handle or ignore empty center configuration gracefully



//  children += new LogViewer:
//    vgrow = Always
//    minWidth = Double.MaxValue
//    minHeight = Double.MaxValue
//    prefWidth = Double.MaxValue
//    prefHeight = Double.MaxValue

object ItsaDebugView:

  import org.itsadigitaltrust.hardwarelogger.services.ReportedIssue as Issue
  import org.itsadigitaltrust.hardwarelogger.core.given


  def report(reporter: Issue => Option[String])(using debugView: ItsaDebugView, showMessage: HWLoggerShowMessage): Unit =
    def sendReport(issue: Issue = Issue()) =
      reporter(makeReport(issue)) match
        case Some(value) => showMessage.showError("Issue Submission failed", value)
        case None => showMessage.showInformation("Issue Reported!")


    def makeReport(issue: Issue = Issue()): Issue =
      val title: String = issue.title
      val description = issue.description.value ?? "" +
        s"""
           |
           |Output
           |==========================================
           |TODO
           |""".stripMargin
      Issue(title, Description(description))
    end makeReport



    showMessage.showConfirmation("Report Issue", "Do you want to report an Issue").match
      case Some(ButtonType.No) => ()
      case Some(ButtonType.Yes | ButtonType.OK) =>
        showMessage.showConfirmation("Customise Issue", "Do you want to customise the issue?").match
          case Some(ButtonType.No) =>  sendReport()
          case Some(ButtonType.Yes | ButtonType.OK) =>
            val dialog = new Dialogs.IssueCustomisationDialog()
            dialog.showDialog()
            if dialog.wasOKed then
              sendReport(dialog.getIssue)
            ()
          case _ => ()
      case _ => ()


end ItsaDebugView


