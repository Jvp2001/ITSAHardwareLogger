package org.itsadigitaltrust.hardwarelogger.ui.derivation

import org.itsadigitaltrust.hardwarelogger.core.DataTypes.*
import org.itsadigitaltrust.hardwarelogger.ui.derivation.annotations.*
import org.itsadigitaltrust.hardwarelogger.core.ui.*

object CustomDataTypeSchemaGivens:
  given UIControlFactory[BatteryHealth] with
    override def createField(initialValue: BatteryHealth, annotations: List[UISchemaAnnotation]): DeferredField =
        val isReadOnly = annotations.exists(_.isInstanceOf[Readonly])
        val initialProperty = StringProperty(initialValue.toString)
        DeferredField(
          controlBuilder = () =>
            if isReadOnly then
              new Label:
                text <== initialProperty
                private val colourStyle = initialValue.getHealthCondition match
                  case BatteryHealthCondition.Replace => "-color-error-fg"
                  case BatteryHealthCondition.Normal => "-color-success-fg"
                  case BatteryHealthCondition.Acceptable => "-color-warning-fg"
                style = s"-fx-text-fill: $colourStyle"
            else
              new TextField:
                text <==> initialProperty

          ,
          () => initialValue
          )
end CustomDataTypeSchemaGivens
