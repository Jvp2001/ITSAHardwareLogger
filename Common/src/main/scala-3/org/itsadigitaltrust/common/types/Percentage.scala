package org.itsadigitaltrust.common.types

import org.itsadigitaltrust.common.Operators.{??, between, in}

import scala.Conversion.into
import scala.compiletime.ops.int.*
import scala.compiletime.*
import scala.compiletime.ops.string.Matches
object PercentageType:
  opaque type Percentage = String
  private type Rhs = Percentage | Double | Int
  extension(rhs: Rhs)
    private[PercentageType] inline def getDouble: Double =
      inline rhs match
        case p: Percentage => p.toDouble
        case d: Double => Percentage(d).toDouble
        case i: Int => Percentage(i).toDouble
  extension (percentage: Percentage)
    inline def > (rhs: Rhs): Boolean =
      val other: Double = rhs.getDouble
      percentage.toDouble > other
      
    inline def < (rhs: Rhs): Boolean =
      val other: Double = rhs.getDouble
      percentage.toDouble < other
      
    inline def == (other: into[Percentage]): Boolean =
      percentage == other

    inline def <= (rhs: Rhs): Boolean =
      val other: Double = rhs.getDouble
      percentage.toDouble <= other

    inline def >= (rhs: Rhs): Boolean =
      val other: Double = rhs.getDouble
      percentage.toDouble >= other
      
    

  object Percentage:


    def apply(value: Double): Percentage =
      if value >= 0.0 && value <= 1.0 then 
        (value * 100).toString + "%"
      else if value >= 1.0 && value <= 100.0 then 
        value.toString + "%"
      else
        "0.0%"
    inline def apply(inline value: Int): Percentage =
      inline if value > 0 && value <= 100 then
      value.toString + "%"
      else
        scala.compiletime.error("Percentage must be between 0 and 100")


    extension(p: Percentage)
      def dbstring: String = p
      def toByte: Byte =
        p.replace("%", "").trim.toByteOption.get
      def toDouble: Double =
        p.replace("%", "").toDoubleOption ?? 0.0
        
      
    extension (i: scala.Int)
      inline def percent: Percentage = Percentage(i)

    extension (s: String)

      inline def percent: Percentage =
        inline if constValue[Matches[s.type, ".*%$"]] then
        s
        else
          scala.compiletime.error("String must end with %")


      def asPercentage: Percentage =
        if s.endsWith("%") then
          if s.startsWith("?") then
            s.replace("?", "-1")
          else
            s
        else
          scala.sys.error("String must end with %")
  end Percentage
  given Conversion [String, Percentage] = _.asPercentage

  extension (d: Double)
    inline def percent: Percentage = Percentage(d)
end PercentageType
export PercentageType.{Percentage,*}
export PercentageType.Percentage.*