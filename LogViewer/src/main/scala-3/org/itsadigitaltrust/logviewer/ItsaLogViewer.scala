package org.itsadigitaltrust.logviewer

import scalafx.geometry.Pos
import org.slf4j.event.Level
import qupath.ui.logviewer.api.LogMessage
import qupath.ui.logviewer.api.manager.LoggerManager
import qupath.ui.logviewer.ui.main.LogViewer
import qupath.ui.logviewer.ui.main.cellfactories.{CompactTableCell, GenericTableCell, LogLevelTableCell, TableRowTableCell}
import scalafx.Includes.*
import scalafx.beans.binding.{Bindings, ObjectBinding, StringBinding}
import scalafx.beans.property.{IntegerProperty, ObjectProperty, ReadOnlyIntegerProperty, StringProperty}
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Orientation
import scalafx.scene.{Cursor, Node}
import scalafx.scene.control.{Button, ComboBox, Label, SelectionMode, SplitPane, TableCell, TableColumn, TableView, TextArea, TextField, ToggleButton, ToolBar, Tooltip}
import scalafx.scene.layout.{BorderPane, HBox, Priority}
import scalafx.event.ActionEvent

import java.io.{PrintWriter, StringWriter}
import java.text.SimpleDateFormat
import java.util
import java.util.{Date, ResourceBundle}
import scala.jdk.CollectionConverters.*

trait LogIconFactory:
  def createIcon(level: Level): Option[Node]
end LogIconFactory

abstract class ItsaLogViewer(using loggerManager: LoggerManager, logIconFactory: LogIconFactory) extends BorderPane:
  final val logViewerModel = new ItsaLogViewerModel(using loggerManager)
  logViewerModel.startLogging()
  final val resources = ResourceBundle.getBundle("strings")
  vgrow = Priority.Always

  private class ToggleLogLevelButton(level: Level) extends ToggleButton:
    tooltip = Tooltip(s"Filter logs by ${level.name().capitalize}")
    text = level.name().capitalize
    onAction = _ =>
      if this.isSelected then
        logViewerModel.displayedLogLevel(level)
      else
        logViewerModel.hideLogLevel(level)
  end ToggleLogLevelButton

  private val traceButton = new ToggleLogLevelButton(Level.TRACE)
  private val debugButton = new ToggleLogLevelButton(Level.DEBUG)
  private val infoButton = new ToggleLogLevelButton(Level.INFO)
  private val warnButton = new ToggleLogLevelButton(Level.WARN)
  private val errorButton = new ToggleLogLevelButton(Level.ERROR)

  private def getButtonForLevel(level: Level): ToggleButton = level match
    case Level.TRACE => traceButton
    case Level.DEBUG => debugButton
    case Level.INFO  => infoButton
    case Level.WARN  => warnButton
    case Level.ERROR => errorButton

  top =
    new ToolBar:
      private val regexButton = new ToggleButton(".*"):
        tooltip = Tooltip("Filter logs by regex.")

      private val messageFilter = new TextField:
        promptText = "Filter by text"

      private val buttonContainer = new HBox(traceButton, debugButton, infoButton, warnButton, errorButton)

      children = ObservableBuffer(regexButton, messageFilter, buttonContainer)
    end new

  private val tableViewLog: TableView[LogMessage] = new TableView[LogMessage]():
    styleClass += "log-table"

    private class LogTableColumn extends TableColumn[LogMessage, LogMessage]:
      cellValueFactory = data => ObjectProperty(data.value)

    private val rowColumn = new LogTableColumn:
      delegate.setCellFactory(_ => new TableRowTableCell(_ => ""))

    private val levelColumn = new LogTableColumn:
      delegate.setCellFactory(_ => new TableCell[LogMessage, LogMessage]:
        item.onChange: (_, _, logMessage) =>
          if logMessage != null then
            text = logMessage.level.name
            graphic =  logIconFactory.createIcon(logMessage.level).orNull
          else
            text = null
            graphic = null

      )

    private val loggerColumn = new LogTableColumn:
      delegate.setCellFactory(_ => new CompactTableCell(_.loggerName, ","))

    private val threadColumn = new LogTableColumn:
      delegate.setCellFactory(_ => new GenericTableCell(_.threadName))

    private val timestampColumn = new LogTableColumn:
      delegate.setCellFactory(_ => new GenericTableCell(logMessage =>
        ItsaLogViewer.TimestampFormat.format(new Date(logMessage.timestamp))
      ))

    private val messageColumn = new LogTableColumn:
      styleClass += "col-message"
      delegate.setCellFactory(_ => new GenericTableCell(_.message))

    columns ++= scala.collection.immutable.List(rowColumn, levelColumn, loggerColumn, threadColumn, timestampColumn, messageColumn)

    selectionModel().selectionMode = SelectionMode.Multiple
    columnResizePolicy = TableView.ConstrainedResizePolicyFlexLastColumn
    items = logViewerModel.filteredLogMessages
    selectionModel().selectedItemProperty().addListener: (_, _, selectedLogMessage) =>
      handleLogMessageSelected(selectedLogMessage)

  end tableViewLog

  private def createCountText(level: Level, messagesCount: ReadOnlyIntegerProperty, singularText: "warning" | "error"): StringBinding =
    val button = getButtonForLevel(level)
    Bindings.createStringBinding(
      () => {
        if messagesCount() == 0 then ""
        else if button.isSelected then
          if messagesCount() == 1 then singularText else s"${messagesCount()} ${singularText}s"
        else ""
      },
      messagesCount,
      button.selectedProperty
    )
  end createCountText

  private class LevelCountLabel(level: Level, countProperty: ReadOnlyIntegerProperty, singularText: "error" | "warning") extends Label:
    managed <== countProperty.isNotEqualTo(0)
    text <== createCountText(level, countProperty, singularText)
    cursor <== javafx.beans.binding.Bindings.createObjectBinding[javafx.scene.Cursor](
      () => if countProperty() != 0 then javafx.scene.Cursor.DEFAULT else javafx.scene.Cursor.HAND,
      countProperty
    )
  end LevelCountLabel

  private final val errorsCount = new LevelCountLabel(Level.ERROR, logViewerModel.logMessageCounts.errorLevelCountsProperty, "error") {
    styleClass += "error-counter"
  }
  private final val warningsCount = new LevelCountLabel(Level.WARN, logViewerModel.logMessageCounts.warnLevelCountsProperty(), "warning") {
    styleClass += "warn-counter"
  }

  private val shownCount = new Label:
    text <==
      Bindings.when(
        logViewerModel.filteredLogsMessageCounts.allLevelCountsProperty().isEqualTo(logViewerModel.logMessageCounts.allLevelCountsProperty())
      ).choose(
        logViewerModel.filteredLogsMessageCounts.allLevelCountsProperty().asString().concat(" ")
          .concat(resources.getString("LogCount.total"))
      ).otherwise(
        logViewerModel.filteredLogsMessageCounts.allLevelCountsProperty().asString().concat("/ ")
          .concat(logViewerModel.logMessageCounts.allLevelCountsProperty().asString())
          .concat(" ")
          .concat(resources.getString("LogCount.shown"))
      )
  end shownCount

  private final val clearLogsButton = new Button("Clear Logs"):
    disable <== javafx.beans.binding.Bindings.size(tableViewLog.delegate.getItems).isEqualTo(0)
    styleClass += "bottom-buttons"

  bottom = new HBox:
    alignment = Pos.CenterLeft
    spacing = 10.0
    BorderPane.setAlignment(this, Pos.Center)
    children.setAll(warningsCount, errorsCount, shownCount, clearLogsButton)

  private val textAreaLog = new TextArea():
    styleClass += "textAreaLog"

  private def getIndexOfLastUnselectedMessage(level: Level): Int =
    val selectedMessages  = tableViewLog.selectionModel().selectedItems.toList
    val displayedMessages = tableViewLog.items().toList

    val lastSelectedMessageWithGivenLevel = selectedMessages.reverseIterator
      .find(_.level == level)

    val startingIndex = lastSelectedMessageWithGivenLevel match
      case Some(msg) => displayedMessages.indexOf(msg)
      case None      => displayedMessages.size

    val indexAboveSelected = (startingIndex - 1 to 0 by -1)
      .find(i => displayedMessages(i).level == level)

    indexAboveSelected match
      case Some(idx) => idx
      case None =>
        (displayedMessages.size - 1 to 0 by -1)
          .find(i => displayedMessages(i).level == level)
          .getOrElse(-1)
  end getIndexOfLastUnselectedMessage

  private var textAreaLogStyleClasses: List[String] = Nil
  private def handleLogMessageSelected(selectedLogMessage: LogMessage): Unit =
    val message = StringBuilder()

    if selectedLogMessage != null then
      message.append(selectedLogMessage.message)
      if selectedLogMessage.throwable != null then
        val sw = new StringWriter()
        sw.write(message.toString)
        textAreaLog.text = sw.toString
        selectedLogMessage.throwable().printStackTrace(new PrintWriter(sw))
        message ++= s"\n$sw"
      textAreaLog.getStyleClass.removeAll(textAreaLogStyleClasses*)
      textAreaLogStyleClasses = toStyleClasses(selectedLogMessage.level)
      textAreaLog.getStyleClass.addAll(textAreaLogStyleClasses*)
    textAreaLog.text = message.toString
  end handleLogMessageSelected

  def toStyleClasses(level: Level): scala.collection.immutable.List[String]

  center = new SplitPane:
    orientation = Orientation.Vertical
    BorderPane.setAlignment(this, Pos.Center)
    children.setAll(tableViewLog, textAreaLog)
    vgrow = Priority.Always
end ItsaLogViewer



object ItsaLogViewer:
  final val TimestampFormat = new SimpleDateFormat(System.getProperty("timestamp.format", "HH:mm:ss"))