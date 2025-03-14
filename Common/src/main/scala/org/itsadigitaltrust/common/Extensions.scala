package org.itsadigitaltrust.common

import scala.collection.IndexedSeqOps
import scala.collection.generic.IsSeq
import scala.util.boundary

extension[T](coll: IndexedSeq[T])
  def filterByIndices(predicate: Int => Boolean): Seq[T] =
    coll.zipWithIndex.filter:(*, index) =>
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

extension[T, E](option: Option[T])(using boundary.Label[E])
  def getOrError(error: E): T =
    option match
      case Some(value) => value
      case None => boundary.break(error)