package org.itsadigitaltrust.hardwarelogger.dialogs

import org.itsadigitaltrust.common.Operators.??

import org.scalafx.extras.generic_pane.GenericDialogFX
import org.itsadigitaltrust.hardwarelogger.core.ui.*

import scalafx.Includes.*
import org.itsadigitaltrust.hardwarelogger.core.BuildInfo
import org.itsadigitaltrust.common.or

private[dialogs] class AboutDialog(ownerWindow: Option[Window] = None) extends GenericDialogFX(title = "About Hardware Logger",
  header = "Hardware Logger",
  ownerWindow = ownerWindow) with GenericDialogFXExtras:

  import java.nio.file.{Files, Paths}
  import java.time.Instant


  private def getJarLastModified: Option[Instant] =
    val path = Paths.get(getClass.getProtectionDomain.getCodeSource.getLocation.toURI)
    if Files.isRegularFile(path) then Some(Files.getLastModifiedTime(path).toInstant)
    else None

  addLabeledText("Name:", BuildInfo.name, 125)
  addLabeledText("Version:", BuildInfo.version.replace("-SNAPSHOT", ""))
  addLabeledText("Scala Version:", BuildInfo.scalaVersion)
  addLabeledText("Build Date:", getJarLastModified.map(_.toString) ?? "Unknown")
  addHyperLink("Organisation:", BuildInfo.organizationName, BuildInfo.organizationHomepage.map(_.toString) ?? "")


end AboutDialog

extension (sd: ShowDialogs)
  def showAboutDialog(ownerWindow: Option[Window] = None): Unit =
    val dialog = new AboutDialog(ownerWindow)
    sd.showGenericDialog(dialog)(Option.apply)