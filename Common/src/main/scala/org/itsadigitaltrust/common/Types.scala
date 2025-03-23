package org.itsadigitaltrust.common

object Types:
  type DataSizeUnit = "KB" | "MB" | "GB" | "TB" | "KiB" | "MiB" | "GiB" | "TiB"
  opaque type DataSize = (Long, DataSizeUnit)

  extension (ds: DataSize)
    def value: Long = ds._1
    def unit: DataSizeUnit = ds._2
    def toString: String =
      s"$value $unit"
    def toLong: Long = value

  object DataSize:
    def apply(value: Long, unit: DataSizeUnit): DataSize =
      (value, unit)



export Types.DataSize
