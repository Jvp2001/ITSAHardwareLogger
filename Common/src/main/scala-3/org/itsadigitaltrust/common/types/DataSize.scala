package org.itsadigitaltrust.common.types

object DataSizeType:

  type DataSizeUnit = "KB" | "MB" | "GB" | "TB" | "KiB" | "MiB" | "GiB" | "TiB"
  private val sizeMap: Map[DataSizeUnit, Double] = Map("KB" -> 1000, "MB" -> 1_000_000, "GB" -> 1_000_000_000, "TB" -> 10_000_000_000L, "KiB" -> 1074, "MiB" -> 1_074_000, "GiB" -> 1_074_000_000, "TiB" -> 1.1e+12)
  opaque type DataSize = (Double, DataSizeUnit)
  extension (ds: DataSize)
    def value: Double = ds._1
    def unit: DataSizeUnit = ds._2
    def dbString: String = s"$value $unit"
    def toString: String =
      s"$value $unit"
    def toDouble: Double = value
    def toBytes: Double = sizeMap(unit) * value
    def to(size: DataSizeUnit): DataSize =
      DataSize(toBytes, size)
    def +(rhs: DataSize): DataSize =
      DataSize(toBytes + rhs.toBytes, unit)
    def -(rhs: DataSize): DataSize =
      DataSize(toBytes - rhs.toBytes, unit)
    def *(rhs: DataSize): DataSize =
      DataSize(toBytes * rhs.toBytes, unit)

  end extension


  given numericDataSize: Numeric[DataSize] = new Numeric[DataSize]:
    override def plus(x: DataSize, y: DataSize): DataSize = x + y

    override def minus(x: DataSize, y: DataSize): DataSize = x - y

    override def times(x: DataSize, y: DataSize): DataSize = x * y

    override def negate(x: DataSize): DataSize = DataSize(-x.value, x.unit)

    override def fromInt(x: Int): DataSize = DataSize(x.toDouble, "GB")

    override def parseString(str: String): Option[(Double, DataSizeUnit)] = DataSize.from(str)

    override def toInt(x: DataSize): Int = x.value.toInt

    override def toLong(x: DataSize): Long = x.value.toLong

    override def toFloat(x: DataSize): Float = x.value.toFloat

    override def toDouble(x: DataSize): Double = x.toDouble

    override def compare(x: DataSize, y: DataSize): Int = x.value.compare(y.value.toDouble)


  object DataSize:
    inline def apply(value: Double, unit: DataSizeUnit): DataSize =
      (value, unit)

    def from(string: String): Option[DataSize] =
      val parts =
        if string.startsWith("(") then
          string.substring(1, string.length - 1).split(",")
        else
          string.split(" ")
      println(s"DataSize.from: $string, parts: ${parts.mkString(",")}")
      if (parts.length != 2) return Option(DataSize(string.toLong, "GB"))
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
      Option(DataSize(value, unit.asInstanceOf[DataSizeUnit]))

end DataSizeType

export DataSizeType.*