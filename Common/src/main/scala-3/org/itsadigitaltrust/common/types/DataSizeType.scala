package org.itsadigitaltrust.common.types

import org.itsadigitaltrust.common.Operators.<=>
import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.common.types.DataSizeType.DataSizeUnit.GiB

object DataSizeType:

  enum DataSizeUnit(val factorToBytes: Double) extends Comparable[DataSizeUnit]:
    case B extends DataSizeUnit(1)
    case KB extends DataSizeUnit(1000)
    case MB extends DataSizeUnit(1_000_000L)
    case GB extends DataSizeUnit(1_000_000_000L)
    case TB extends DataSizeUnit(1_000_000_000_000L)
    case PB extends DataSizeUnit(1_000_000_000_000_000L)
    case KiB extends DataSizeUnit(1024)
    case MiB extends DataSizeUnit(1024 * 1024)
    case GiB extends DataSizeUnit(1024 * 1024 * 1024)
    case TiB extends DataSizeUnit(1024 * 1024 * 1024 * 1024)
    case PiB extends DataSizeUnit(1024 * 1024 * 1024 * 1024 * 1024)

    def >(rhs: DataSizeUnit): Boolean =
      factorToBytes > rhs.factorToBytes

    def <(rhs: DataSizeUnit): Boolean =
      factorToBytes < rhs.factorToBytes

    def ==(rhs: DataSizeUnit): Boolean =
      factorToBytes == rhs.factorToBytes

    def !=(rhs: DataSizeUnit): Boolean =
      !(this == rhs)

    override def compareTo(o: DataSizeUnit): Int =
      if this < o then
        -1
      else if this == o then
        0
      else
        1
    end compareTo

  end DataSizeUnit

  opaque type DataSize = (value: Double, unit: DataSizeUnit)

  given Conversion[Seq[DataSize], Seq[Double]] = _.map(_.value)

  given Conversion[DataSize, Double] = _.value

  given Conversion[DataSize, Long] = _.toLong

  object DataSize:
    inline def apply(): DataSize = DataSize.default

    def apply(value: Double, unit: DataSizeUnit, inBytes: Boolean = true): DataSize =
      if inBytes && unit != DataSizeUnit.B then
        (value, DataSizeUnit.B).asInstanceOf[DataSize].toSize(unit)
      else
        (value, unit)

    def from(string: String): Option[DataSize] =
      val parts = string.split(" ")
      if parts.length != 2 then None
      else
        val value = parts(0).toLongOption
        val unit = DataSizeUnit.values.find(_.toString == parts(1))
        for v <- value; u <- unit
          yield DataSize(v, u)

    end from

    def default: DataSize = (0, DataSizeUnit.MB)

  end DataSize

  extension (ds: DataSize)
    def unit: DataSizeUnit = ds.unit
    def value: Double = ds.value

    inline def toDouble: Double = ds.toLong.toDouble
    inline def toBytes: Long = (value * unit.factorToBytes).toLong
    inline def toSize(targetUnit: DataSizeUnit): DataSize =
      val newValue = toBytes / targetUnit.factorToBytes
      (newValue, targetUnit)
    inline def toLong: Long = ds.value.toLong
    def dbString: String =
      val newUnit = if unit == DataSizeUnit.GiB then DataSizeUnit.GB else unit
      val result = s"${value.toString.replaceFirst("(e|E[\\-?]\\d+)|(\\[e|E\\d+])", "").replaceFirst("\\[\\]", "").replace(".", "")} $newUnit"
      if result.endsWith(s"0 $newUnit") then result.replace(s"0 $newUnit", s" $newUnit") else result
    def +(rhs: DataSize): DataSize = DataSize((toBytes + rhs.toBytes) / unit.factorToBytes, unit)
    def -(rhs: DataSize): DataSize = DataSize((toBytes - rhs.toBytes) / unit.factorToBytes, unit)
    def *(rhs: Double): DataSize = DataSize(value * rhs, unit)
    def add(rhs: DataSize): DataSize = DataSize((toBytes + rhs.toBytes) / unit.factorToBytes, unit)


    inline def toGiBString: String =
      s"${ds.toSize(DataSizeUnit.GiB).toLong} GiB"

    inline def toGBString: String =
      s"${ds.toSize(DataSizeUnit.GB).toLong} GB"

  end extension

  given numericDataSize: Numeric[DataSize] with
    override def plus(x: DataSize, y: DataSize): DataSize = x + y

    override def minus(x: DataSize, y: DataSize): DataSize = x - y

    override def times(x: DataSize, y: DataSize): DataSize = DataSize(x.toBytes * y.toBytes / x.unit.factorToBytes, x.unit)

    override def negate(x: DataSize): DataSize = DataSize(-x.value, x.unit)

    override def fromInt(x: Int): DataSize = DataSize(x.toLong, DataSizeUnit.B)

    override def parseString(str: String): Option[DataSize] = DataSize.from(str)

    override def toInt(x: DataSize): Int = x.value.toInt

    override def toLong(x: DataSize): Long = x.value.toLong

    override def toFloat(x: DataSize): Float = x.value.toFloat

    override def toDouble(x: DataSize): Double = x.value

    override def compare(x: DataSize, y: DataSize): Int = x.toBytes.compare(y.toBytes)
  end numericDataSize

  extension (seq: Seq[DataSize])
    def total: DataSize =
      seq.sum
    def toTotalGBString: String =
      seq.total.toGBString
    def toTotalGiBString: String =
      seq.total.toGiBString
    // ✅ Clean utility to aggregate directly into a distinct target unit type safely
    def sumAs(targetUnit: DataSizeUnit): DataSize =
      val totalBytes = seq.map(_.toBytes).sum
      DataSize(totalBytes / targetUnit.factorToBytes, targetUnit, inBytes = false)
  end extension
end DataSizeType


