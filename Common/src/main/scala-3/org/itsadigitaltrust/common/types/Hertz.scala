package org.itsadigitaltrust.common.types




object FrequencyType:
  enum FrequencyUnit(factorToHertz: Long):
    case Hz extends FrequencyUnit(1L)
    case kHz extends FrequencyUnit(1000L)
    case MHz extends FrequencyUnit(1000000L)
    case GHz extends FrequencyUnit(1000000000L)

    def frequency: Long = factorToHertz

    def dbString: String = this match
      case FrequencyUnit.Hz => "Hz"
      case FrequencyUnit.kHz => "kHz"
      case FrequencyUnit.MHz => "MHz"
      case FrequencyUnit.GHz => "GHz"


    override def toString: String =
      dbString

  end FrequencyUnit

  opaque type Frequency = (value: Double, unit: FrequencyUnit)

  extension (freq: Frequency)
    def toFrequency(unit: FrequencyUnit): Frequency = (freq.value * unit.frequency, unit)
    def dbString: String = s"${freq.value} ${freq.unit.dbString.replaceAll("e|E\\d", "")}"
  end extension

  object Frequency:
    inline def apply(value: Double, unit: FrequencyUnit): Frequency = (value, unit)
    def apply(value: String): Frequency =
      fromString(value).getOrElse(throw new IllegalArgumentException(s"Invalid frequency string: $value"))
    def fromString(string: String): Option[Frequency] =
      val parts = string.split(" ")
      if parts.length != 2 then None
        //if parts.length == 1 && parts(0).matches("$*(hz|khz|mhz|ghz)$")
      else
        val value = parts(0).toDoubleOption
        val unit = FrequencyUnit.values.find(_.dbString == parts(1))
        for v <- value; u <- unit yield Frequency(v, u)
    end fromString


  end Frequency
end FrequencyType


export FrequencyType.{FrequencyUnit, Frequency, *}