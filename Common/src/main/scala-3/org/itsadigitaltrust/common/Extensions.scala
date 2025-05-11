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

  import scala.compiletime.ops.int.*

  extension (long: Long)
    @targetName("pow")
    infix def **(raiseBy: Long): Long =
      Math.pow(long, raiseBy).toLong
    def KiB: DataSize = DataSize(long / 1024, "KiB")
    def MiB: DataSize = DataSize(long / (1024 ** 2), "MiB")
    def GiB: DataSize = DataSize(long / (1024 ** 3), "GiB")
    def TiB: DataSize = DataSize(long / (1024 ** 4), "TiB")

    def KB: DataSize = DataSize(long / 1000, "KB")
    def MB: DataSize = DataSize(long / (1000 ** 2), "MB")
    def GB: DataSize = DataSize(long / (1000 ** 3), "GB")
    def TB: DataSize = DataSize(long / (1000 ** 4), "TB")
export Maths.*

object StringExtensions:
  extension (s: String)
    def startWithIgnoreCase(string: String): Boolean =
      s.toLowerCase.startsWith(string)

end StringExtensions
export StringExtensions.*