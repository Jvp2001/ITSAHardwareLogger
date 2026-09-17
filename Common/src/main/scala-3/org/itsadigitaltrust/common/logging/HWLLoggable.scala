package org.itsadigitaltrust.common.logging

import org.itsadigitaltrust.common.Operators.notIn
import com.typesafe.scalalogging.{CanLog, LazyLogging, Logger, LoggerImpl, LoggerTakingImplicitImpl, StrictLogging}

import scala.collection.mutable
import scala.deriving.Mirror
import scala.reflect.ClassTag


// When a function needs a logger, this can be given to it via a using parameter
type HWLLogger = Logger
trait HWLLoggable extends LazyLogging:
  // Allow the logger to be passed to a function or method, as a context parameter.
  protected final given log: HWLLogger = logger

object HWLLoggable

