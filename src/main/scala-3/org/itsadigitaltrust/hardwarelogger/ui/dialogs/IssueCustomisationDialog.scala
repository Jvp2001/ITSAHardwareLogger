package org.itsadigitaltrust.hardwarelogger.ui.dialogs

import org.itsadigitaltrust.hardwarelogger.issuereporter.{Description, ReportedIssue}
import org.itsadigitaltrust.hardwarelogger.services.IssueReporterService

import org.scalafx.extras.generic_pane.GenericDialogFX
import scalafx.scene.control.{TextArea, TextField}
import org.itsadigitaltrust.hardwarelogger.services.ReportedIssue

import scala.annotation.targetName

class IssueCustomisationDialog extends GenericDialogFX("Customise ReportedIssue"):
  private val reportTitle = new TextField:
    promptText = "Title"
  addNode("Title", reportTitle)

  private val area = new TextArea("")
  addNode("Description", area)

  def getDescription: String = area.text.value

  def getIssue: ReportedIssue =
    ReportedIssue(reportTitle.text.value, Description(getDescription))

  def value: ReportedIssue = getIssue

end IssueCustomisationDialog


inline def showIssueCustomisationDialog(): Unit =
  new IssueCustomisationDialog().showDialog()

