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

extension [T, E](option: Option[T])(using boundary.Label[E])
  def getOrError(error: E): T =
    option match
      case Some(value) => value
      case None => boundary.break(error)

end extension

object Maths:

  extension (long: Long)
    @targetName("pow")
    infix def **(raiseBy: Long): Long =
      Math.pow(long, raiseBy).toLong
    def KiB: Long = long / 1024
    def MiB: Long = long / (1024 ** 2)
    def GiB: Long = long.MiB / (1024 ** 3)
    def TiB: Long = long.GiB / (1024 ** 4)

    def KB: Long = long / 1000
    def MB: Long = long.KB / (1000 ** 2)
    def GB: Long = long.MB / (1000 ** 3)
    def TB: Long = long.GB / (1000 ** 4)
