package org.itsadigitaltrust.hardwarelogger.core

import org.itsadigitaltrust.common.types.PercentageType.Percentage

object DataTypes:

    enum BatteryHealthCondition:
      case Normal, Acceptable, Replace

      def colour: String = this match
        case BatteryHealthCondition.Replace => "-color-error-fg"
        case BatteryHealthCondition.Normal => "-color-success-fg"
        case BatteryHealthCondition.Acceptable => "-color-warning-fg"
    opaque type BatteryHealth = Percentage

    extension (health: BatteryHealth)
      def getHealthCondition: BatteryHealthCondition =
        if health.toDouble > 75.0 then BatteryHealthCondition.Normal
        else if health.toDouble > 50.0 then BatteryHealthCondition.Acceptable
        else BatteryHealthCondition.Replace

    object BatteryHealth:
      inline def apply(value: Double): BatteryHealth = Percentage(value)

    inline given Conversion[Double, BatteryHealth] = Percentage.apply
    given Conversion[BatteryHealth, Double] = _.toDouble
    
    given Conversion[BatteryHealth, Percentage] = identity
    export Percentage.*
end DataTypes

  
