package org.itsadigitaltrust.hardwarelogger.services

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import org.itsadigitaltrust.common.InputStreamExtensions.readAllAsString
import org.itsadigitaltrust.common.Operators.*
import org.itsadigitaltrust.common.{ReflectionUtils, config}
import org.itsadigitaltrust.hardwarelogger.core.BuildInfo
import org.itsadigitaltrust.hardwarelogger.core.Givens.issueReporterDefaults
import org.itsadigitaltrust.hardwarelogger.{issuereporter, services}
import org.itsadigitaltrust.hardwarelogger.issuereporter.{GitHubIssuePayload, GitHubIssueReporter, GitHubIssueTransformer, IssueReportStatus, ReportedIssue}
import org.itsadigitaltrust.hardwarelogger.ui.derivation.annotations.Ignore
import org.slf4j.LoggerFactory
import ox.either.{fail, ok}
import qupath.ui.logviewer.api.manager.LoggerManager

import scala.collection.JavaConverters.{asScalaBufferConverter, asScalaIteratorConverter}
import scala.compiletime.constValueTuple
import scala.deriving.Mirror
import scala.util.Try


export org.itsadigitaltrust.hardwarelogger.issuereporter.ReportedIssue


enum IssueSeverity:
  case Low, Medium, High
enum IssueLabel:
  case Bug, FeatureRequest, Enhancement

trait Issue(title: String, body: String, val labels: List[IssueLabel], severity: IssueSeverity)

object Issue:

  final case class BugReport(title: String, body: String, @Ignore logs: String = "", @Ignore severity: IssueSeverity = IssueSeverity.Medium) extends Issue(title, body, List(IssueLabel.Bug), severity)
  final case class FeatureRequest(title: String, body: String, severity: IssueSeverity = IssueSeverity.Medium) extends Issue(title, body, List(IssueLabel.FeatureRequest), severity)
  final case class Enhancement(title: String, body: String, severity: IssueSeverity = IssueSeverity.Medium) extends Issue(title, body, List(IssueLabel.Enhancement), severity)

  private inline def buildInfo(using M: Mirror.ProductOf[BuildInfo.type]): String =
    val map: Map[String, Any] = ReflectionUtils.toMap[BuildInfo.type](BuildInfo)
    val contents = map.map((key, value) => s"$key: $value").mkString("\n")
    s"Build Info\n==============================================\n$contents\n\n"

  given issuePayloadTransformer: GitHubIssueTransformer[Issue] with
    private def getLogs: String =
      val context = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
      val sb = new StringBuilder()

      // 1. Target and extract the cached backend logs
      val rootLogger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
      val appenders = rootLogger.iteratorForAppenders().asScala

      // Locate the structural memory appender containing our historical elements
      val rawLogsDump = appenders.find(a => a.getName == "LIST" || a.getClass.getSimpleName.contains("ListAppender")) match
        case Some(appender) =>
          try
            val getListMethod = appender.getClass.getMethod("getList")
            val logList = getListMethod.invoke(appender).asInstanceOf[java.util.List[ILoggingEvent]].asScala

            // Format log lines into a clean text block
            logList.map: event =>
              val time = java.time.Instant.ofEpochMilli(event.getTimeStamp)
              s"[$time] [${event.getLevel}] [${event.getLoggerName}] - ${event.getFormattedMessage}"
            .mkString("\n")
          catch
            case _: Exception => "Error: Unable to extract data array via reflection."
        case _ =>
          "Error: Memory Logback ListAppender target was not found."

      end rawLogsDump
      rawLogsDump
    end getLogs

    override def toPayload(issue: Issue): GitHubIssuePayload =
      issue match
        case Issue.BugReport(title, body, logs, severity) =>
          val buildInfoString = buildInfo
          val newBody =
            s"""$body
               |
               |${buildInfoString}
               |
               |$getLogs
               |""".stripMargin
          GitHubIssuePayload(
            title = title,
            body = newBody,
            labels = issue.labels.map(_.toString),
            severity = severity.toString
          )
        case feature@Issue.FeatureRequest(title, body, severity) =>
          GitHubIssuePayload(
            title = title,
            body = body,
            labels = issue.labels.map(_.toString),
            severity = severity.toString
          )
        case Issue.Enhancement(title, body, severity) =>
          GitHubIssuePayload(
            title = title,
            body = body,
            labels = issue.labels.map(_.toString),
            severity = severity.toString
          )
    end toPayload


trait IssueReporterService extends FrontendService:
  def report(issue: Issue): Either[String, Unit]

trait IssueReporterServiceComponent:
  given issueReporterService: IssueReporterService = compiletime.deferred

private[services] class GitHubIssueReporterService extends IssueReporterService:
  lazy val issueReporter: Either[Throwable, GitHubIssueReporter] =
    config"issuereporter.properties".toString |>
    GitHubIssueReporter.apply

  override def report(issue: Issue): Either[String, Unit] =
    logger.info("Reporting...")
    val result = issueReporter
      .left.map(error => Option(error.getMessage).getOrElse(error.toString))
      .flatMap: reporter =>
        reporter.report(issue) match
          case IssueReportStatus.Success => Right(())
          case IssueReportStatus.Error(message) =>
            Left(message.getOrElse("GitHub rejected the issue report without an error message."))

    result match
      case Right(_) => logger.info("GitHub issue report submitted successfully.")
      case Left(error) => logger.error(s"Could not submit GitHub issue report: $error")
    result

