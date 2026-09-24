package org.itsadigitaltrust.hardwarelogger.ui.dialogs
import javafx.scene.control.ListCell
import org.itsadigitaltrust.common.Operators.{??, toOption}
import org.itsadigitaltrust.hardwarelogger.core.ui.*
import org.itsadigitaltrust.hardwarelogger.services.validation.ValidationIssue
import scalafx.scene.control.{ListView, SelectionModel}
import scalafx.stage.StageStyle
class DetailedValidationAlert private(`type`: AlertType, issues: Seq[ValidationIssue]) extends Alert(`type`):
    title = "Validation Warnings"
    headerText = "The following validation warnings were found"
    contentText = "Click 'Show details' to see the validation issues."

    val listView = new ListView[ValidationIssue]:
      vgrow = Always
      items.get ++= issues

      delegate.setCellFactory(lv =>
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

    initStyle(StageStyle.Decorated)
end DetailedValidationAlert

object DetailedValidationAlert:
  def showAsError(issues: Seq[ValidationIssue]): Unit =
    new DetailedValidationAlert(AlertType.Error, issues):
      title = "Validation Errors"
      headerText = "Click more to see the validation errors."

      buttonTypes = Seq(ButtonType.OK)
    .showAndWait()
    .get
    ()
  end showAsError

  def showAsWarning(issues: Seq[ValidationIssue]): Boolean =
    new DetailedValidationAlert(AlertType.Warning, issues)
    .showAndWait()
    .map:
      case ButtonType.Yes => true
      case _ => false
    ?? false



