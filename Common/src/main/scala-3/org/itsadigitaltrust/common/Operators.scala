package org.itsadigitaltrust.common

import java.util.Comparator
import scala.annotation.{tailrec, targetName}
import scala.collection.generic.{IsMap, IsSeq}
import scala.collection.mutable
import scala.reflect.ClassTag
import scala.util.Try
import scala.util.chaining.scalaUtilChainingOps
import scala.language.experimental.genericNumberLiterals

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

  extension[E, T](either: Either[E, T])
    def ??(default: => T): T =
      either.getOrElse(default)
  extension[T](t: T)
    def ? : Option[T] = Option(t)
    def toOption: Option[T] = Option(t)

  extension[T](option: Option[T])
    def ??(default: => T): T =
      option.getOrElse(default)

  extension[T](option: Option[T] | T | Try[T] | Try[Option[T]])

    infix def or(other: Option[T]): Option[T] =
      option match
        case opt: Option[T] => opt.orElse(other)
        case t: T => other.orElse(t.?)
        case t: Try[Option[T]] => t.getOrElse(other)
        case t: Try[T] => t.toOption.orElse(other)
  end extension

  extension[A](a: A)
    inline def |>[B](inline f: A => B): B =
      a.pipe(f)

    inline def ||>[C,B](inline f: C ?=> A => B)(using C): B =
      a.pipe(_ => f(a))
  extension [A](inline option: Option[A])
    inline def |> [B](inline f: A => B): Option[B] =
      option.map(a => f(a))

    inline def ||>[C,B](inline f: C ?=> A => B)(using C): Option[B] =
      option.map(a => f(a))
  end extension

  extension[T <: Comparable[T]](lhs: T)
    def <=>(rhs: T): Int =
      lhs.compareTo(rhs)


  extension(s: String | Null)
    def ??(other: String): String =
      if s == null || s.isBlank then
        other
      else 
        s


end Operators



export Operators.{or, in, notIn, <=>, |>}