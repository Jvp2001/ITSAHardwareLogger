package org.itsadigitaltrust.common

import scala.annotation.targetName

object Types:
  type DataSizeUnit = "KB" | "MB" | "GB" | "TB" | "KiB" | "MiB" | "GiB" | "TiB"
  opaque type DataSize = (Long, DataSizeUnit)

  extension (ds: DataSize)
    def value: Long = ds._1
    def unit: DataSizeUnit = ds._2
    def dbString: String = s"${value} ${unit}"
    def toString: String =
      s"$value $unit"
    def toLong: Long = value

  object DataSize:
    inline def apply(value: Long, unit: DataSizeUnit): DataSize =
      (value, unit)

  opaque type Percentage = String

  import scala.compiletime.ops.int.*
  import scala.compiletime.*
  object Percentage:

    inline def apply(inline value: Int): Percentage =
      inline if value > 0 && value <= 100 then
        value.toString + "%"
      else
        scala.compiletime.error("Percentage must be between 0 and 100")

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
        s
      else
        scala.sys.error("String must end with %")


export Types.{percent, asPercentage,  *}
