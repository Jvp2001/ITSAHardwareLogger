package org.itsadigitaltrust.logviewer

import org.slf4j.event.Level
import qupath.ui.logviewer.api.LogMessage
import qupath.ui.logviewer.api.listener.LoggerListener
import qupath.ui.logviewer.api.manager.LoggerManager
import qupath.ui.logviewer.ui.main.LogMessageCounts
import scalafx.application.Platform
import scalafx.beans.property.{BooleanProperty, StringProperty}
import scalafx.collections.{ObservableBuffer, ObservableSet}
import scalafx.collections.transformation.FilteredBuffer

final class ItsaLogViewerModel(using loggerManager: LoggerManager) extends LoggerListener:
  final val allLogMessages = ObservableBuffer[LogMessage]()
  final val filteredLogMessages = FilteredBuffer[LogMessage](allLogMessages)
  final val logMessageCounts = new LogMessageCounts(allLogMessages)
  final val filteredLogsMessageCounts = new LogMessageCounts(filteredLogMessages)
  private val filterByRegexProperty =  BooleanProperty(false)
  private val filterProperty = StringProperty("")
  private val allThreads = ObservableSet[String]()
  private val displayedThreads = ObservableSet[String]()
  private val displayAllThreadsProperty = BooleanProperty(true)
  private val displayedLogLevels = ObservableSet(Level.values.toList*)

  def startLogging(): Unit =
    loggerManager.addListener(this)

  def stopLogging(): Unit =
    loggerManager.removeListener(this)

  override def addLogMessage(logMessage: LogMessage): Unit =
    if Platform.isFxApplicationThread then
      allLogMessages += logMessage
    else
      Platform.runLater(() => allLogMessages += logMessage)

  def displayedLogLevel(level: Level): Unit =
    displayedLogLevels += level

  def hideLogLevel(level: Level): Unit =
    displayedLogLevels -= level