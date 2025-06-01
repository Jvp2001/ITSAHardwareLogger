package org.itsadigitaltrust.common

import scala.annotation.targetName
import scala.collection.IndexedSeqOps
import scala.collection.generic.IsSeq
import scala.util.boundary
import org.itsadigitaltrust.common.types.DataSizeType.DataSizeUnit

extension [T](coll: IndexedSeq[T])
  def filterByIndices(predicate: Int => Boolean): Seq[T] =
    coll.zipWithIndex.filter: (*, index) =>
      predicate(index)
    .map: (item, *) =>
      item

  def getEvenIndexItems: Seq[T] =
    filterByIndices: index =>
      index % 2 == 0

  def getOddIndexItems: Seq[T] =
    filterByIndices: index =>
      index % 2 != 0
end extension


object Maths:


  extension (double: Double)
    @targetName("pow")
    infix def **(raiseBy: Double): Double =
      Math.pow(double, raiseBy)
    def KiB: DataSize = DataSize(double, DataSizeUnit.KiB)
    def MiB: DataSize = DataSize(double, DataSizeUnit.MiB)
    def GiB: DataSize = DataSize(double, DataSizeUnit.GiB)
    def TiB: DataSize = DataSize(double, DataSizeUnit.TiB)
    def PiB: DataSize = DataSize(double, DataSizeUnit.PiB)
    def KB: DataSize = DataSize(double, DataSizeUnit.KB)
    def MB: DataSize = DataSize(double, DataSizeUnit.MB)
    def GB: DataSize = DataSize(double, DataSizeUnit.GB)
    def TB: DataSize = DataSize(double, DataSizeUnit.TB)
    def PB: DataSize = DataSize(double, DataSizeUnit.PB)
    def B: DataSize = DataSize(double, DataSizeUnit.B)
end Maths

export Maths.*


object StringExtensions:
  extension (s: String)
    def startWithIgnoreCase(string: String): Boolean =
      s.toLowerCase.startsWith(string)

end StringExtensions
export StringExtensions.*