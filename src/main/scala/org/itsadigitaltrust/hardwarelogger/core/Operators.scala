package org.itsadigitaltrust.hardwarelogger.core

import scala.collection.mutable


object Operators:
  type MapType[K, V] = Map[K, V] | mutable.Map[K, V]

  extension[T](item: T)
    infix def in(seq: Seq[T]): Boolean =
      seq.contains(item)
    infix def notIn(seq: Seq[T]): Boolean =
      !seq.contains(item)
    infix def in[V](seq: MapType[T, V]): Boolean =
      seq.contains(item)
    infix def notIn[V](seq: MapType[T, V]): Boolean =
      !seq.contains(item)


