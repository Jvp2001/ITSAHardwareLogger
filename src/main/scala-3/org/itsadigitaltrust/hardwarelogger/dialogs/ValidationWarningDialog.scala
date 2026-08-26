package org.itsadigitaltrust.hardwarelogger.dialogs
import javafx.scene.control.ListCell
import javafx.util.Callback
import org.itsadigitaltrust.hardwarelogger.core.ui.*
import org.itsadigitaltrust.hardwarelogger.services.validation.ValidationIssue
import scalafx.scene.control.{Dialog, ListView}
import scalafx.stage
class ValidationWarningDialog(warnings: Seq[ValidationIssue]) extends Alert(AlertType.Warning):
  title = "Validation Warnings"
  headerText = "The following validation warnings were found:"

  val listView = new ListView[ValidationIssue]:
    items.get ++= warnings
    delegate.setCellFactory(  lv =>
      new ListCell[ValidationIssue]:
        override def updateItem(item: ValidationIssue, empty: Boolean): Unit =
          super.updateItem(item, empty)
          if item ne null then
            setText(item.message)
    )
  end listView


  val content = new VBox():
    children += listView
    prefWidth = 400
    prefHeight = 300
  end content

  dialogPane.delegate.get.setExpandableContent(content)
  dialogPane.delegate.get.setExpanded(true)

  initOwner(null)

  buttonTypes = Seq(ButtonType.Yes, ButtonType.No)

end ValidationWarningDialog





