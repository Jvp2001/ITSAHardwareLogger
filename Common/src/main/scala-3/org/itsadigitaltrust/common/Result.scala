package org.itsadigitaltrust.common
import scala.languageFeature.implicitConversions
import scala.util.boundary

sealed trait Result[+T, +E]:
  def toEither: Either[E, T] =
    this match
      case Success(value) => util.Right(value)
      case Error(reason) => util.Left(reason)

  def mapSuccess[R](f: T => R): Result[R, E] =
      Result.fromEither.apply(this.toEither.map(f))


object Result:
  given fromEither[T, E]: Conversion[Either[E, T], Result[T, E]] with

    override def apply(x: Either[E, T]): Result[T, E] =
      Result:
        x match
          case Left(value) => error(value)
          case Right(value) => success(value)

  final case class Success[+T](value: T) extends Result[T, Nothing]

  final case class Error[+E](reason: E) extends Result[Nothing, E]


  inline def withContinuation[T, E](inline body: Continuation[T, E] ?=> T): Result[T, E] =
    boundary: label ?=>
      Success(body(using Continuation(label)))

  inline def apply[T, E](inline body: Continuation[T, E] ?=> T): Result[T, E] =
    withContinuation(body)


  final class Continuation[T, E](private[Result] val label: boundary.Label[Result[T, E]])
  
  //  private[Result] object Continuation:
  def error[T, E](error: E)(using continuation: Continuation[T, E]): Nothing =
    boundary.break(Error(error))(using continuation.label)

  def success[T, E](value: T)(using continuation: Continuation[T, E]): Nothing =
    boundary.break(Success(value))(using continuation.label)
end Result

export Result.{Success, Error}

extension [T](option: Option[T])
  def getOrError[E](error: E)(using continuation: Result.Continuation[T, E]): Result[T, E] =
    option match
      case Some(value) => Result.success(value)
      case None => Result.error(error)
