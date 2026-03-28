package org.itsadigitaltrust.hardwarelogger.dialogs

import org.scalafx.extras.generic_pane.GenericDialogFX
import org.itsadigitaltrust.hardwarelogger.ui.*

import scalafx.scene.control.Label
trait GenericDialogFXExtras:
  this: GenericDialogFX =>

  def addHyperLink(label: String,  name: String, url: String): Unit =
    addNode(label, new WebSiteHyperLink(name, url))

  def addLabeledText(label: String, text: String): Unit =
    addNode(label, new Label(text))
  def addLabeledText(label: String, text: String, textMinWidth: Int): Unit =
    addNode(label, new Label(text):
      minWidth = textMinWidth
    )

end GenericDialogFXExtras
