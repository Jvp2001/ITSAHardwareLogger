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

import java.nio.charset.Charset

class ItsaDebugView(using issueReporterService: IssueReporterService) extends ConsoleView:
  System.setOut(getOut)
  System.setErr(getOut)

  createAndAddItem("Report", _ => ItsaDebugView.report(issueReporterService.report)(using this))


object ItsaDebugView:

  import org.itsadigitaltrust.hardwarelogger.services.Issue
  import org.itsadigitaltrust.hardwarelogger.core.given

  def report(reporter: Issue => Option[String])(using debugView: ItsaDebugView): Unit =
    def sendReport(issue: Issue = Issue()) =
      reporter(makeReport(issue)) match
        case Some(value) => Dialogs.showErrorAlert("Issue Submission failed", value)
        case None => Dialogs.showInfoAlert("Issue Reported!", "")


    def makeReport(issue: Issue = Issue()): Issue =
      val title: String = issue.title
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
        case Some(ButtonType.No) => ()
        case Some(ButtonType.Yes | ButtonType.OK) =>
          Dialogs.createConfirmationAlert("Customise Issue", "Do you want to customise the issue?").showAndWait().match
            case Some(ButtonType.No) =>  sendReport()
            case Some(ButtonType.Yes | ButtonType.OK) =>
              Dialogs.showIssueCustomisationDialog(): dialog =>
                sendReport(makeReport(dialog.getIssue))
            case _ => ()
        case _ => ()



  end report
