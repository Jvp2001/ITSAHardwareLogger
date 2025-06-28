package org.itsadigitaltrust.hardwarelogger.views

import javafx.scene.control.TextFormatter
import javafx.util.converter.DefaultStringConverter
import jp.uphy.javafx.console.ConsoleView
import org.itsadigitaltrust.common.Operators.{??, |>}

import scalafx.beans.property.StringProperty
import org.itsadigitaltrust.hardwarelogger.core.ui.*
import org.itsadigitaltrust.hardwarelogger.issuereporter.Description
import org.itsadigitaltrust.hardwarelogger.services.{Issue, IssueReporterService}
import org.scalafx.extras.auto_dialog.AutoDialog
import scalafx.scene.control.Alert.AlertType.{Confirmation, Information}
import scalafx.scene.control.{ContextMenu, Dialog, MenuItem, SeparatorMenuItem, TextArea}
import scalafx.scene.input.{Clipboard, ClipboardContent}
import scalafx.util.StringConverter

class ItsaDebugView(using issueReporterService: IssueReporterService) extends ConsoleView:
  System.setErr(getOut)
  System.setOut(getOut)

  createAndAddItem("Report", _ => ItsaDebugView.report(issueReporterService.report)(using this))

//TODO: Generate token key. Attach errors.


object ItsaDebugView:
  import org.itsadigitaltrust.hardwarelogger.core.given
  def report(reporter: Issue => Option[String])(using debugView: ItsaDebugView): Unit =
    def sendReport(issue: Issue = Issue()) =
      reporter(makeReport(issue)) match
        case Some(value) => Dialogs.showErrorAlert( "Issue Submission failed", value)
        case None => Dialogs.showInfoAlert("Issue Reported!", "")


    def makeReport(issue: Issue = Issue()) =
      val title = if issue.title.isEmpty then "Issue" else issue.title
      val description = issue.description.value ?? "" +
        s"""
           |
           |Output
           |==========================================
           |${debugView.getOutput}
           |""".stripMargin
      Issue(title, Description(description))
    end makeReport
     Dialogs.createConfirmationAlert("Report Issue", "Do you want to report an Issue").showAndWait().match
        case ButtonType.No => ()
        case ButtonType.Yes | ButtonType.OK =>
          Dialogs.createConfirmationAlert("Customise Issue", "Do you want to customise the issue?").showAndWait().match
            case ButtonType.No =>  sendReport()
            case ButtonType.Yes | ButtonType.OK =>
              Dialogs.showIssueCustomisationDialog(): dialog =>
                sendReport(makeReport(dialog.getIssue))


  end report
