package org.itsadigitaltrust.common

import ox.Ox

import scala.annotation.targetName
import scala.util.boundary.Label

/**
 * A type to help the refactoring of the code that was in here into a separate package.
 * This allows me to not have ot go through the rest of my code base and change the imports.
 */
object Types:
  export types.DataSizeType.*
  export types.PercentageType.*
  export types.Frequency.*
  export types.DataSizeType
  export Percentage.*

  type PrimitiveType = String | Float | Long | Double | Short | Boolean
  type Result[+T, +E] = Either[E, T]


  object Result:
    
    type Success[T] = Right[Nothing, T]
    inline def apply[T, E](value: T): Result[T, E] = Right(value)
    inline def apply[A, E](inline body: Label[Either[E, A]] ?=> A): Either[E, A] =
      ox.either(body)

    inline def catchAll[T](inline t: Label[Either[Throwable, T]] ?=> T): Either[Throwable, T] =
      ox.either.catchAll(t)

    inline def ok[T](value: T): Result[T, Nothing] = Right(value)


export Types.*

