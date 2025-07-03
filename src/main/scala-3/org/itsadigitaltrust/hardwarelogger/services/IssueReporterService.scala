package org.itsadigitaltrust.hardwarelogger.services

import org.itsadigitaltrust.common.InputStreamExtensions.readAllAsString
import org.itsadigitaltrust.common.Operators.??
import org.itsadigitaltrust.common.Result

import org.itsadigitaltrust.hardwarelogger.services
import org.itsadigitaltrust.hardwarelogger.issuereporter.{Description, GitHubIssueReporter, IssueReportStatus, ReportedIssue}

import scala.util.Try


export org.itsadigitaltrust.hardwarelogger.issuereporter.ReportedIssue 
extension (issue: ReportedIssue)
  def name: String = issue.title



trait IssueReporterService:
  def report(issue: ReportedIssue): Option[String]  =
    report(issue.name, issue.description.value)
  def report(name: String, description: String): Option[String]

class StandardIssueReporterService extends IssueReporterService:
  import org.itsadigitaltrust.hardwarelogger.core.issueReporterDefaults
  lazy val issueReporter: GitHubIssueReporter =
    GitHubIssueReporter(Try(getClass.getResourceAsStream("src/main/resources/org/itsadigitaltrust/hardwarelogger/services/issuesReporter.properties").readAllAsString()))

  override def report(name: String, description: String): Option[String] =
      issueReporter.report(ReportedIssue(name, Description(description))) match
        case IssueReportStatus.Success => None
        case IssueReportStatus.Error(message) => message


