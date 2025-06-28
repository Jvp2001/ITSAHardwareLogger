package org.itsadigitaltrust.common
import scala.languageFeature.implicitConversions
import scala.util.boundary

sealed trait Result[+T, +E] extends Matchable, Product, Equals:
  import Result.!

  def toEither: Either[E, T] =
    this match
      case Success(value) => util.Right(value)
      case Error(reason) => util.Left(reason)

//  def convertError[U >: T,  E2](f: E => E2)(using Result.![U, E2]): Result.![U, E2] =
//      this match
//        case Success(value) => Result.success[U, E2](value)
//        case Error(reason) => Result.error(f(reason))
//


  def toOption: Option[T] =
    this match
      case Success(value) => Some(value)
      case Error(_) => None
  def toOptionError: Option[E] =
    this match
      case Success(_) => None
      case Error(e) => Option(e)
  def success: T =
    this match
      case Success(value) => value
      case Error(reason) => scala.sys.error(reason.toString)

object Result:
  type ![T, E] = Result.Continuation[T, E]



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
