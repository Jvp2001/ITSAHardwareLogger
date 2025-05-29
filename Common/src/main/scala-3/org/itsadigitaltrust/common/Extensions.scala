package org.itsadigitaltrust.common

import scala.annotation.targetName
import scala.collection.IndexedSeqOps
import scala.collection.generic.IsSeq
import scala.util.boundary

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
      Math.pow(double, raiseBy).toLong
    def KiB: DataSize = DataSize(double / 1024, "KiB")
    def MiB: DataSize = DataSize(double / 1.049e6, "MiB")
    def GiB: DataSize = DataSize(double / 1074e9, "GiB")
    def TiB: DataSize = DataSize(double / 1.1e+12, "TiB")

    def KB: DataSize = DataSize(double * 1e-3, "KB")
    def MB: DataSize = DataSize(double * 1e-6, "MB")
    def GB: DataSize = DataSize(double * 1e-9, "GB")
    def TB: DataSize = DataSize(double * 1e-4, "TB")
end Maths

export Maths.*


object StringExtensions:
  extension (s: String)
    def startWithIgnoreCase(string: String): Boolean =
      s.toLowerCase.startsWith(string)

end StringExtensions
export StringExtensions.*