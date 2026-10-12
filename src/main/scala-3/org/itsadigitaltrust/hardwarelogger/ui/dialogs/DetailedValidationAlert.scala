package org.itsadigitaltrust.hardwarelogger.ui.dialogs
import javafx.scene.control.ListCell
import org.itsadigitaltrust.common.Operators.{??, toOption}
import org.itsadigitaltrust.hardwarelogger.core.ui.*
import org.itsadigitaltrust.hardwarelogger.services.validation.ValidationIssue
import scalafx.scene.control.{ListView, SelectionModel}
import scalafx.stage.StageStyle
import scala.jdk.CollectionConverters.*

class DetailedValidationAlert private(`type`: AlertType, issues: Seq[ValidationIssue]) extends Alert(`type`):
    private val ownerWindow = javafx.stage.Window.getWindows.asScala.find(_.isFocused)
    ownerWindow.foreach(window => initOwner(window))

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
      prefWidth = ownerWindow.map(_.getWidth).getOrElse(400.0)
      prefHeight = 300
    end content

    dialogPane.delegate.get.setExpandableContent(content)
    dialogPane.delegate.get.setExpanded(true)

    buttonTypes = Seq(ButtonType.Yes, ButtonType.No)

    initStyle(StageStyle.Decorated)

    delegate.setOnShown(_ =>
      ownerWindow.foreach: owner =>
        val alertWindow = dialogPane.delegate.get.getScene.getWindow
        alertWindow.setWidth(owner.getWidth)
        alertWindow.setX(owner.getX)
    )
end DetailedValidationAlert

object DetailedValidationAlert:
  private val saveButtonType = new ButtonType("Save")
  private val cancelButtonType = new ButtonType("Cancel")

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
    val alert = new DetailedValidationAlert(AlertType.Warning, issues):
      buttonTypes = Seq(saveButtonType, cancelButtonType)
    alert.showAndWait().exists(_ == saveButtonType)
