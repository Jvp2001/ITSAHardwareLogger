package org.itsadigitaltrust.common.collections

trait CaseIterable[+T]:
  val cases: Seq[T]

  def foreach(f: T => Unit): Unit =
    cases.foreach(f)

  def map[U](f: T => U): Seq[U] =
    cases.map(f)

  def apply(index: Int): T = cases(index)

end CaseIterable

