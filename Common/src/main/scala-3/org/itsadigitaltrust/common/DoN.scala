package org.itsadigitaltrust.common

class DoN[T](times: Long)(private val f: () => T):
  var count = 0L
  def apply(): Unit =
    if count < times then
      f()
      count += 1
  def reset(): Unit =
    count = 0L

end DoN

final class DoOnce[T](f: => () => T) extends DoN(1)(f)
