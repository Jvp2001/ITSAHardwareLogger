package org.itsadigitaltrust.common.types

object DataSizeType:

  type DataSizeUnit = "B" | "KB" | "MB" | "GB" | "TB" | "KiB" | "MiB" | "GiB" | "TiB"
  private val sizeMap: Map[DataSizeUnit, Double] = Map(
    "KB" -> 1000,
    "MB" -> 1_000_000,
    "GB" -> 1_000_000_000,
    "TB" -> 10_000_000_000L,
    "KiB" -> 1074,
    "MiB" -> 1_074_000,
    "GiB" -> 1_074_000_000,
    "TiB" -> 1.1e+12)
  opaque type DataSize = (Double, DataSizeUnit)
  extension (ds: DataSize)
    def value: Double = ds._1
    def unit: DataSizeUnit = ds._2
    def dbString: String = s"$value $unit"
    def toString: String =
      s"$value $unit"
    def toDouble: Double = value
    def toBytes: Double = sizeMap(unit) * value
    def toSize(size: DataSizeUnit): DataSize =
      val currentUnit =  ds._2
      val newValue = (currentUnit, size) match
        case ("B", "KB") => value * 1e-1
        case ("B", "MB") => value * 1e-6
        case ("B", "GB") => value * 1e-9
        case ("B", "TB") => value * 1e-12
        case ("KB", "B") => value * 1e1
        case ("KB", "MB") => value * 1e-3
        case ("KB", "GB") => value * 1e-6
        case ("KB", "TB") => value * 1e-9
        case ("MB", "B") => value * 1e6
        case ("MB", "KB") => value * 1e3
        case ("MB", "GB") => value * 1e-3
        case ("MB", "TB") => value * 1e-6
        case ("GB", "B") => value * 1e9
        case ("GB", "KB") => value * 1e6
        case ("GB", "MB") => value * 1e3
        case ("GB", "TB") => value * 1e-3
        case ("TB", "B") => value * 1e12
        case ("TB", "KB") => value * 1e9
        case ("TB", "MB") => value * 1e6
        case ("TB", "GB") => value * 1e3
        case ("KiB", "B") => value * 1024
        case ("KiB", "MiB") => value * 1e-3
        case ("KiB", "GiB") => value * 1e-6
        case ("KiB", "TiB") => value * 1e-9
        case ("MiB", "B") => value * 1_048_576
        case ("MiB", "KiB") => value * 1024
        case ("MiB", "GiB") => value * 1e-3
        case ("MiB", "TiB") => value * 1e-6
        case ("GiB", "B") => value * 1_073_741_824
        case ("GiB", "KiB") => value * 1_048_576
        case ("GiB", "MiB") => value * 1024
        case ("GiB", "TiB") => value * 1e-3
        case ("TiB", "B") => value * 1_099_511_627_776l
        case ("TiB", "KiB") => value * 1_073_741_824
        case ("TiB", "MiB") => value * 1_048_576
        case ("TiB", "GiB") => value * 1024
        case _ => throw new IllegalArgumentException(s"Cannot convert from $currentUnit to $size")
      end newValue
      DataSize(newValue, size)
    end toSize

    
        
    
          
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