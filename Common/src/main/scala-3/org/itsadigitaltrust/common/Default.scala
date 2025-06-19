package org.itsadigitaltrust.common

import scala.annotation.tailrec
import scala.deriving.Mirror
import scala.quoted.*

trait Default[T]:
  def default: T


object Default:
  given Default[Boolean] with
    override def default: Boolean = false

  given[T <: Numeric[T]]: Default[T] with
    override def default: T = 0.asInstanceOf[T]

  given Default[String] with
    override def default: String = ""
