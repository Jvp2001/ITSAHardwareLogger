package org.itsadigitaltrust.hardwarelogger.views

import org.itsadigitaltrust.common.Operators.??
import org.itsadigitaltrust.common.OSUtils
import org.itsadigitaltrust.common.processes.proc

import org.itsadigitaltrust.hardwarelogger.issuereporter.Description
import org.itsadigitaltrust.hardwarelogger.services.Issue

import org.scalafx.extras.generic_pane.GenericDialogFX
import scalafx.scene.control.Alert.AlertType.Confirmation
import scalafx.scene.control.{Alert, ButtonType, DConvert, Dialog, DialogPane, Label, TextArea, TextField}
import scalafx.stage.FileChooser
import scalafx.Includes.*
import scalafx.Includes.given
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.layout.GridPane

import scala.jdk.CollectionConverters.*
import java.io.File
import java.nio.charset.Charset
import java.nio.file.{Files, Path}
import scala.collection.{immutable, mutable}

object Dialogs:

  import org.itsadigitaltrust.hardwarelogger.services.{Issue, IssueReporterService}

  class IssueCustomisationDialog extends GenericDialogFX("Customise Issue"):

    private val reportTitle = new TextField:
      promptText = "Title"
    addNode("Title", reportTitle)

    private val area = new TextArea("")
    addNode("Description", area)

    def getDescription: String = area.text.value

    def getIssue: Issue =
      Issue(reportTitle.text.value, Description(getDescription))

    def value: Issue = getIssue

    showDialog()
  end IssueCustomisationDialog


  def showIssueCustomisationDialog()(okay: IssueCustomisationDialog => Unit): Unit =
    val dialog = new IssueCustomisationDialog()
    dialog.showDialog()
    if dialog.wasOKed then
      okay(dialog)

  def showErrorAlert(alertHeader: String, message: String): Unit =
    createAlert(AlertType.Error, alertHeader, message)
      .showAndWait()


  def showInfoAlert(alertHeader: String, message: String): Option[ButtonType] =
    createAlert(AlertType.Information, alertHeader, message)
      .showAndWait()

  private def createAlert(`type`: AlertType, alertHeader: String, message: String) =
    new Alert(`type`):
      title = `type`.toString
      headerText = alertHeader
      contentText = message

  def createConfirmationAlert(alertTitle: String, message: String, buttonPair: "YesNo" | "OkCancel" = "YesNo"): Alert =
    val buttons = buttonPair match
      case "YesNo" => Seq(ButtonType.Yes, ButtonType.No)
      case "OkCancel" => Seq(ButtonType.OK, ButtonType.Cancel)
    new Alert(Confirmation):
      title = alertTitle
      contentText = message
      buttonTypes = buttons
  end createConfirmationAlert

  def showConfirmationAlert(alertHeader: String, message: String): Option[ButtonType] =
    val alert = createConfirmationAlert("Confirmation", message)
    alert.headerText = alertHeader
    alert.contentText = message
    alert.showAndWait()

  def saveDialog[C](dialogTitle: String, contents: C, fileTypes: String*)(save: (File, C) => Unit): Unit =
    val fileChooser = new scalafx.stage.FileChooser:
      title = dialogTitle
      private val filters = fileTypes.map(ft => scalafx.stage.FileChooser.ExtensionFilter(ft, s"*$ft"))
      filters.map(_.delegate).foreach(extensionFilters.add)
    val file = Option(fileChooser.showSaveDialog(null))
    file match
      case Some(value) => save(value, contents)
      case None => ()
  end saveDialog

  def saveTextFile(title: String, contents: String): Unit =
    saveDialog(title, contents, "txt"): (file, _) =>
      Files.writeString(Path.of(file.getAbsolutePath), contents, Charset.defaultCharset())
      val folder = file.getParentFile.getAbsolutePath
      if OSUtils.onWindows then
        proc"start $folder"
      else
        proc"open $folder"
end Dialogs
