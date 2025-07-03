package org.itsadigitaltrust.hardwarelogger.dialogs

import org.itsadigitaltrust.common.OSUtils
import org.itsadigitaltrust.common.Operators.??
import org.itsadigitaltrust.common.processes.proc

import org.itsadigitaltrust.hardwarelogger.issuereporter.Description
import org.itsadigitaltrust.hardwarelogger.services.ReportedIssue

import org.scalafx.extras.generic_pane.GenericDialogFX
import scalafx.Includes.{*, given}
import scalafx.scene.control.*
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.Alert.AlertType.Confirmation
import scalafx.scene.layout.GridPane
import scalafx.stage.FileChooser

import java.io.File
import java.nio.charset.Charset
import java.nio.file.{Files, Path}
import scala.collection.{immutable, mutable}
import scala.jdk.CollectionConverters.*




object Dialogs extends ShowDialogs:

  import org.itsadigitaltrust.hardwarelogger.services.ReportedIssue
  import org.itsadigitaltrust.hardwarelogger.dialogs
  import dialogs.*
  export dialogs.{HardDriveExtraInformationDialog, IssueCustomisationDialog, showIssueCustomisationDialog, showHardDriveExtraInfoDialog}
end Dialogs
