package org.itsadigitaltrust.hardwarelogger.ui.derivation
import org.itsadigitaltrust.hardwarelogger.core.ui.*
import org.scalafx.extras.generic_pane.GenericPane
import scalafx.geometry.{HPos, Orientation}
import scalafx.scene.control.Separator
object UIGeneration:
  def createInfoPane[A: UISchema as schema](initialData: A, verticalGap: Double = 10.0, horizontalGap: Double = 10): GridPane =
    val numberOfColumns = 2
    val (fields, reconstructor) = schema(initialData)
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

  end createInfoPane


end UIGeneration




