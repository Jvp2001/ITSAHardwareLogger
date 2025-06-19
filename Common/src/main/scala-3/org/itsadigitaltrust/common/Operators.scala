package org.itsadigitaltrust.common

import scala.collection.generic.{IsMap, IsSeq}
import scala.collection.mutable
import scala.util.chaining.scalaUtilChainingOps



object Operators:
  type MapType[K, V] = Map[K, V] | mutable.Map[K, V]
  
  extension [T](item: T)
    infix def in(seq: Seq[T]): Boolean =
      seq.contains(item)
    infix def notIn(seq: Seq[T]): Boolean =
      !seq.contains(item)
    infix def in[V](seq: MapType[T, V]): Boolean =
      seq.contains(item)
    infix def notIn[V](seq: MapType[T, V]): Boolean =
      !seq.contains(item)
  end extension
  extension[T](option: Option[T])
    infix def ??(default: T): T =
      option.getOrElse(default)

  extension[T](n: Null)
    infix def ??(other: T): T =
      other
  extension[T](value: T | Option[T])
    infix def ??(other: T): T =
      val result = value match
        case t: T => Option(t)
        case option:Option[T] => option
      result.getOrElse(other)

  extension[A](a: A)
    def |>[B](f: A => B): B =
      a.pipe(f)


  extension[T <: Comparable[T]](lhs: T)
    def <=>(rhs: T): Int =
      lhs.compareTo(rhs)
end Operators



export Operators.*