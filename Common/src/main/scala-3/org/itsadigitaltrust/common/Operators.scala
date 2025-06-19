package org.itsadigitaltrust.common

import scala.collection.generic.{IsMap, IsSeq}
import scala.collection.mutable
import scala.reflect.ClassTag
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

  extension[T : ClassTag ](option: Option[T] | T)
    infix def ??(default: T): T =
      option match
        case opt:Option[T] => opt.getOrElse(default)
        case t:T => if t == null then default else t

    infix def ??(other: Option[T]): Option[T] =
      option match
        case opt: Option[T] => opt.orElse(other)
        case t: T => if t == null then other else Some(t)

  end extension
  extension[A](a: A)
    def |>[B](f: A => B): B =
      a.pipe(f)


  extension[T <: Comparable[T]](lhs: T)
    def <=>(rhs: T): Int =
      lhs.compareTo(rhs)
end Operators



export Operators.{??, in, notIn, <=>, |>}