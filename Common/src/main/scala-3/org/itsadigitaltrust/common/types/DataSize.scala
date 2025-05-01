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

  def from(string: String): DataSize =
    val parts =
      if string.startsWith("(") then
        string.substring(1, string.length - 1).split(",")
      else
        string.split(" ")
    println(s"DataSize.from: $string, parts: ${parts.mkString(",")}")
    if (parts.length != 2) return DataSize(string.toLong, "GB")
    val value = parts(0).toLong
    val unit = parts(1) match
      case "KB" => "KB"
      case "MB" => "MB"
      case "GB" => "GB"
      case "TB" => "TB"
      case "KiB" => "KiB"
      case "MiB" => "MiB"
      case "GiB" => "GiB"
      case "TiB" => "TiB"
      case _ => throw new IllegalArgumentException(s"Unknown DataSize unit: ${parts(1)}")
    DataSize(value, unit.asInstanceOf[DataSizeUnit])