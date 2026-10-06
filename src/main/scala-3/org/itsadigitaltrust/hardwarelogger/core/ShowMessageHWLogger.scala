
package org.itsadigitaltrust.hardwarelogger.core

import org.itsadigitaltrust.common.logging.HWLLoggable
import org.scalafx.extras.{ShowMessage, ShowMessageLogger}
import scalafx.stage.Window

trait ShowMessageHWLogger extends ShowMessageLogger, HWLLoggable:
  override def warn(message: String): Unit = logger.warn(message)

  override def error(message: String): Unit = logger.error(message)

  override def error(message: String, t: Throwable): Unit = logger.error(message, t)

end ShowMessageHWLogger


trait HWLoggerShowMessage extends ShowMessageHWLogger, ShowMessage:
  override protected def messageLogger: Option[ShowMessageLogger] = Option(this)

  override protected def parentWindow: Option[Window] = None

  
  def showInformation(message: String): Unit = showInformation("Info", message, "")


