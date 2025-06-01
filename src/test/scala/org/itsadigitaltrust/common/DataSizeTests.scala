package org.itsadigitaltrust.common

import org.itsadigitaltrust.common.types.DataSizeType.DataSizeUnit
import org.scalatest.funsuite.AnyFunSuite

class DataSizeTests extends AnyFunSuite:
  import org.itsadigitaltrust.common.types.DataSizeType.*

  test("DataSize toString and dbString"):
    val ds = DataSize(1, DataSizeUnit.MB)
    assert(ds.toString == "1 MB")
    assert(ds.dbString == "1 MB")


  test("DataSize toBytes"):
    val ds = DataSize(1, DataSizeUnit.MB)
    assert(ds.toBytes == 1_000_000)


  test("DataSize conversion to different units"):
    val ds = DataSize(1, DataSizeUnit.MB)
    assert(ds.toSize(DataSizeUnit.KB).value == 1000.0)
    assert(ds.toSize(DataSizeUnit.GB).value == 0.001)
    assert(ds.toSize(DataSizeUnit.B).value == 1_000_000.0)

  // generate tests to check all conversions between these units B, KB, MB, GB, TB, PB, KiB, MiB, GiB, TiB, PiB
  test("DataSize conversions between all units"):
    val units = List[DataSizeUnit](DataSizeUnit.KB, DataSizeUnit.MB, DataSizeUnit.GB, DataSizeUnit.TB, DataSizeUnit.PB, DataSizeUnit.KiB, DataSizeUnit.MiB, DataSizeUnit.GiB, DataSizeUnit.TiB, DataSizeUnit.PiB)
    val perms = units.grouped(2).toSeq.permutations.toSeq
    perms.flatten.foreach: perm =>
      val fromUnit = perm.head
      val toUnit = perm.last
      val ds = DataSize(1, fromUnit)
      val converted = ds.toSize(toUnit)
      assert(converted.value == (ds.toBytes / toUnit.factorToBytes), s"Conversion from $fromUnit to $toUnit failed")

  test("DataSize toDouble"):
    val ds = DataSize(1, DataSizeUnit.MB)
    assert(ds.toDouble == 1.0)
  test("DataSize value and unit"):
    val ds = DataSize(1, DataSizeUnit.MB)
    assert(ds.value == 1.0)
    assert(ds.unit == DataSizeUnit.MB)

