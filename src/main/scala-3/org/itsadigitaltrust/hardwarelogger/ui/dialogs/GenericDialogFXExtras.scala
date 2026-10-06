package org.itsadigitaltrust.hardwarelogger.ui.dialogs

import org.scalafx.extras.generic_pane.{GenericDialogFX, GenericPaneBase}
import org.itsadigitaltrust.hardwarelogger.ui.*
import scalafx.beans.property.StringProperty
import scalafx.scene.control.{Label, TextArea}
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