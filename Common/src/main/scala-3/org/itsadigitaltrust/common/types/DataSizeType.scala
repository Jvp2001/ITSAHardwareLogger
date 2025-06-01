package org.itsadigitaltrust.common.types

object DataSizeType:

  enum DataSizeUnit(val factorToBytes: Double):
    case B extends DataSizeUnit(1)
    case KB extends DataSizeUnit(1e3)
    case MB extends DataSizeUnit(1e6)
    case GB extends DataSizeUnit(1e9)
    case TB extends DataSizeUnit(1e12)
    case PB extends DataSizeUnit(1e15)
    case KiB extends DataSizeUnit(1024)
    case MiB extends DataSizeUnit(1024 * 1024)
    case GiB extends DataSizeUnit(1024 * 1024 * 1024)
    case TiB extends DataSizeUnit(1024 * 1024 * 1024 * 1024)
    case PiB extends DataSizeUnit(1024 * 1024 * 1024 * 1024 * 1024)

  opaque type DataSize = (Double, DataSizeUnit)

  object DataSize:
    def apply(value: Double, unit: DataSizeUnit): DataSize = (value, unit)

    def from(string: String): Option[DataSize] =
      val parts = string.split(" ")
      if parts.length != 2 then None
      else
        val value = parts(0).toDoubleOption
        val unit = DataSizeUnit.values.find(_.toString == parts(1))
        for v <- value; u <- unit yield DataSize(v, u)
  end DataSize
  
  extension (ds: DataSize)
    def value: Double = ds._1
    def unit: DataSizeUnit = ds._2
    def toBytes: Double = value * unit.factorToBytes
    def toSize(targetUnit: DataSizeUnit): DataSize =
      val newValue = toBytes / targetUnit.factorToBytes
      DataSize(newValue, targetUnit)
    def toDouble: Double = value
    def dbString: String = s"$value $unit"
    def +(rhs: DataSize): DataSize = DataSize((toBytes + rhs.toBytes) / unit.factorToBytes, unit)
    def -(rhs: DataSize): DataSize = DataSize((toBytes - rhs.toBytes) / unit.factorToBytes, unit)
    def *(rhs: Double): DataSize = DataSize(value * rhs, unit)
    def default: DataSize = (0, DataSizeUnit.MB)
  end extension

  given numericDataSize: Numeric[DataSize] with
    def plus(x: DataSize, y: DataSize): DataSize = x + y
    def minus(x: DataSize, y: DataSize): DataSize = x - y
    def times(x: DataSize, y: DataSize): DataSize = DataSize(x.toBytes * y.toBytes / x.unit.factorToBytes, x.unit)
    def negate(x: DataSize): DataSize = DataSize(-x.value, x.unit)
    def fromInt(x: Int): DataSize = DataSize(x.toDouble, DataSizeUnit.B)
    def parseString(str: String): Option[DataSize] = DataSize.from(str)
    def toInt(x: DataSize): Int = x.value.toInt
    def toLong(x: DataSize): Long = x.value.toLong
    def toFloat(x: DataSize): Float = x.value.toFloat
    def toDouble(x: DataSize): Double = x.value
    def compare(x: DataSize, y: DataSize): Int = x.toBytes.compare(y.toBytes)
  end numericDataSize


end DataSizeType

export DataSizeType.{DataSizeUnit, *}
