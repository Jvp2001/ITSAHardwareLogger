package org.itsadigitaltrust.common.configformats


import java.util.Properties
import scala.deriving.Mirror
import scala.util.Try

trait PropertyReader[A]:
  def read(props: Properties, prefix: String): Either[String, A]

object PropertyReader:
  // Automatically derives the reader for any case class using macros
  inline given derived[A](using Mirror.ProductOf[A]): PropertyReader[A] =
    PropertyReaderMacros.deriveReader[A]

  // Primitive Instances
  given PropertyReader[String] with
    def read(props: Properties, key: String): Either[String, String] =
      Option(props.getProperty(key)).toRight(s"Missing required property: $key")

  given PropertyReader[Int] with
    def read(props: Properties, key: String): Either[String, Int] =
      Option(props.getProperty(key))
        .toRight(s"Missing required property: $key")
        .flatMap(v => Try(v.toInt).toEither.left.map(_ => s"Invalid Int format for key: $key"))

  given PropertyReader[Boolean] with
    def read(props: Properties, key: String): Either[String, Boolean] =
      Option(props.getProperty(key))
        .toRight(s"Missing required property: $key")
        .flatMap(v => Try(v.toBoolean).toEither.left.map(_ => s"Invalid Boolean format for key: $key"))

  // Native handling for Option types
  given [A](using reader: PropertyReader[A]): PropertyReader[Option[A]] with
    def read(props: Properties, key: String): Either[String, Option[A]] =
      if !props.containsKey(key) then Right(None)
      else reader.read(props, key).map(Some(_))

