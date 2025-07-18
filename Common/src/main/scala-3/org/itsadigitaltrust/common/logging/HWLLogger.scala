package org.itsadigitaltrust.common.logging

import org.itsadigitaltrust.common.Operators.notIn

import com.typesafe.scalalogging.{CanLog, Logger, LoggerImpl, LoggerTakingImplicitImpl, StrictLogging}

import scala.collection.mutable
import scala.reflect.ClassTag

type HWLLoggerImpl = LoggerImpl
class HWLLogger private[logging] extends StrictLogging:
  def apply(): HWLLoggerImpl = logger

object HWLLogger:
  private var loggers = mutable.Map[String, HWLLogger]()

  def apply(klazz: Class[?]): HWLLoggerImpl =
    val ct = ClassTag(klazz)
    val name = ct.runtimeClass.getName
    if name notIn loggers then
      loggers(name) = new HWLLogger
    loggers(name)()

  def apply[T : ClassTag]: HWLLoggerImpl =
    val ct = summon[ClassTag[T]]
    val name = ct.runtimeClass.getName
    if name notIn loggers then
      loggers(name) = new HWLLogger
    loggers(name)()

  def apply(name: String): HWLLoggerImpl =
    if name notIn loggers then
      loggers(name) = new HWLLogger
    loggers(name)()

end HWLLogger



trait HWLLoggable:
  protected given loggable: this.type = this
  protected val loggerName: String = getClass.getName
  protected given logger: HWLLoggerImpl =
    HWLLogger(loggerName)

  def apply(): HWLLoggerImpl = logger


object HWLLoggable:
  given default: HWLLoggable = new HWLLoggable:
    override protected val loggerName: String =  "Default"