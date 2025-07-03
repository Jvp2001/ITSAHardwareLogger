package org.itsadigitaltrust.hardwarelogger.dialogs

import org.itsadigitaltrust.common.OSUtils
import org.itsadigitaltrust.common.Operators.??
import org.itsadigitaltrust.common.processes.proc

import org.itsadigitaltrust.hardwarelogger.issuereporter.Description
import org.itsadigitaltrust.hardwarelogger.services.ReportedIssue
import Dialogs.IssueCustomisationDialog

import org.scalafx.extras.generic_pane.GenericDialogFX
import scalafx.Includes.{*, given}
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.Alert.AlertType.Confirmation
import scalafx.scene.control.*
import scalafx.scene.layout.GridPane
import scalafx.stage.FileChooser

import java.io.File
import java.nio.charset.Charset
import java.nio.file.{Files, Path}
import scala.annotation.targetName
import scala.collection.{immutable, mutable}
import scala.jdk.CollectionConverters.*


private[dialogs] trait CreateDialogs:

  def createAlert(`type`: AlertType, alertHeader: String, message: String): Alert =
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
end CreateDialogs


private[dialogs] trait ShowDialogs extends CreateDialogs:


  def showErrorAlert(alertHeader: String, message: String): Unit =
    createAlert(AlertType.Error, alertHeader, message)
      .showAndWait()


  def showInfoAlert(alertHeader: String, message: String): Option[ButtonType] =
    createAlert(AlertType.Information, alertHeader, message)
      .showAndWait()

  def showConfirmationAlert(alertHeader: String, message: String): Option[ButtonType] =
    val alert = createConfirmationAlert("Confirmation", message)
    alert.headerText = alertHeader
    alert.contentText = message
    alert.showAndWait()

  def showDBConnectionError(): Unit =
    showErrorAlert("Could not connect to database!", "Failed to connect to the database; please check your intranet connection, and try again!")
  

  @targetName("showGenericDialogWithHandler")
  def showGenericDialog[D <: GenericDialogFX, R](dialog: D)(handler: D => Option[R] ): Option[R] =
    dialog.showDialog()
    handler(dialog)

end ShowDialogs


