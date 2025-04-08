package org.itsadigitaltrust.common.types

type DataSizeUnit = "KB" | "MB" | "GB" | "TB" | "KiB" | "MiB" | "GiB" | "TiB"
opaque type DataSize = (Long, DataSizeUnit)
extension (ds: DataSize)
  def value: Long = ds._1
  def unit: DataSizeUnit = ds._2
  def dbString: String = s"$value $unit"
  def toString: String =
    s"$value $unit"
  def toLong: Long = value

object DataSize:
  inline def apply(value: Long, unit: DataSizeUnit): DataSize =
    (value, unit)