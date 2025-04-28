package org.itsadigitaltrust.common


import scala.util.boundary


sealed trait Result[+T, +E]

final case class Success[+T](value: T) extends Result[T, Nothing]

final case class Error[+E] (reason: E) extends Result[Nothing, E]

object Result:
  def withContinuation[T, E](x: Continuation[T, E] ?=> T): Result[T, E] =
    boundary: label ?=>
      Success(x(using Continuation(label)))

  def apply[T, E](x: Continuation[T, E] ?=> T): Result[T, E] =
    withContinuation(x)


  final class Continuation[T, E](private[Result] val label: boundary.Label[Result[T, E]])

//  private[Result] object Continuation:
  def error[T, E](error: E)(using continuation: Continuation[T, E]): Nothing =
    boundary.break(Error(error))(using continuation.label)

  def success[T, E](value: T)(using continuation: Continuation[T, E]): Nothing =
    boundary.break(Success(value))(using continuation.label)


extension[T](option: Option[T])
  def getOrError[E](error: E)(using continuation: Result.Continuation[T, E]): Result[T, E] =
    option match
      case Some(value) => Result.success(value)
      case None => Result.error(error)
