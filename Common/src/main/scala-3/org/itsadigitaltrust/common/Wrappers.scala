package org.itsadigitaltrust.common

object Wrappers:
  def toNoArgLambdaSeq[R](functions: => (() => R)*): Seq[() => R] =
    functions.map: f =>
      () => f()

