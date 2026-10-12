package org.itsadigitaltrust.hardwarelogger.ui.derivation
import org.itsadigitaltrust.common.Operators.?
import org.itsadigitaltrust.hardwarelogger.core.ui.*
import org.scalafx.extras.generic_pane.GenericPane
import scalafx.geometry.{HPos, Orientation}
import scalafx.scene.control.{Dialog, Separator}
import javafx.scene.control
import javafx.util.Callback => jfxsc
object UIGeneration:
  def createInfoPane[A: UISchema as schema](initialData: A, verticalGap: Double = 10.0, horizontalGap: Double = 10): GridPane =
    val (fields, _) = schema(initialData)
    createPaneFromFields(fields, verticalGap, horizontalGap)

  private def createPaneFromFields[A](fields: List[FieldUI[A]], verticalGap: Double, horizontalGap: Double): GridPane =
    val numberOfColumns = 2
    new GridPane():
      this.vgap = verticalGap
      this.hgap = horizontalGap

      var rowIndex = 0
      fields.foreach: fieldUI =>

          val resolvedLabelText =
            fieldUI
              .getAnnotation[annotations.DisplayName].map(_.name)
              .getOrElse(fieldUI.label.fromCamelCase)

          val labelNode = new Label(s"$resolvedLabelText:")
          val controlNode = fieldUI.field.controlBuilder()
          controlNode.id = fieldUI.label

          val hasSeparatorAnnotation = fieldUI.hasAnnotation[annotations.Separator]
          if hasSeparatorAnnotation then
            val separator =  Separator(Orientation.Horizontal)
            separator.maxWidth = Double.MaxValue
            GridPane.setHgrow(separator, Priority.Always)
            GridPane.setHalignment(separator, HPos.Center)
            GridPane.setColumnSpan(separator, numberOfColumns)

            add(separator, 0, rowIndex)
            rowIndex += 1

          add(labelNode, 0, rowIndex)

          add(controlNode, 1, rowIndex)
          rowIndex += 1

    end new

  end createPaneFromFields

  /**
   * Generates a fully functional ScalaFX Dialog containing the derived form fields.
   * Clicking the Save button flushes the inputs and outputs a cleanly typed instance of A.
   */
  def createDialog[A: UISchema as schema](
                                           initialData: A,
                                           titleText: String = "Edit Details",
                                           header: Option[String] = None,
                                           verticalGap: Double = 10.0,
                                           horizontalGap: Double = 10.0,
                                           successButtonLabel: String = "Submit"
                                         ): javafx.scene.control.Dialog[Option[A]] =

    val (fields, reconstructor) = schema(initialData)
    val formPane = createPaneFromFields(fields, verticalGap, horizontalGap)

    val dialog = new javafx.scene.control.Dialog[Option[A]]()
    dialog.setHeaderText(header.getOrElse(""))
    dialog.getDialogPane.setContent(formPane)

    // Explicitly pull the ScalaFX ButtonType primitives
    val saveButtonType = new ButtonType(successButtonLabel, ButtonType.OK.buttonData)
    val cancelButtonType = ButtonType.Cancel
    dialog.getDialogPane.buttonTypes.setAll(saveButtonType, cancelButtonType)


    // FIX: Explicitly match against the underlying JavaFX platform delegates (.delegate)
    dialog.setResultConverter(new javafx.util.Callback[javafx.scene.control.ButtonType, Option[A]]:
      override def call(buttonType: javafx.scene.control.ButtonType): Option[A] =
        if buttonType == saveButtonType.delegate then
          try
            // Safely execute the closure to collect form UI elements back into type A
            val result = reconstructor().?
            println(s"Successfully reconstructed object: $result")
            result
          catch
            case ex: Exception =>
              println(s"Error reconstructing data payload layout: ${ex.getMessage}")
              ex.printStackTrace() // Useful to print structural layout cast failures
              None
        else
          None
      )
    dialog
  end createDialog

end UIGeneration




