package org.itsadigitaltrust.common.types


import scala.compiletime.ops.int.*
import scala.compiletime.*
object PercentageType:
  opaque type Percentage = String
  object Percentage:

    inline def apply(inline value: Int): Percentage =
      inline if value > 0 && value <= 100 then
      value.toString + "%"
      else
        scala.compiletime.error("Percentage must be between 0 and 100")

    extension(p: Percentage)
      def dbstring: String = p
      def toByte: Byte =
        p.replace("%", "").trim.toByteOption.get
    extension (i: Int)
      inline def percent: Percentage = Percentage(i)

    import scala.compiletime.ops.string.*
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

end PercentageType
export PercentageType.{Percentage,*}
export Percentage.{percent, asPercentage, dbstring}