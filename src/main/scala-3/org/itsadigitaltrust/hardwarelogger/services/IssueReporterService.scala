package org.itsadigitaltrust.hardwarelogger.services

import org.itsadigitaltrust.common.Operators.??
import org.itsadigitaltrust.common.Result
import org.itsadigitaltrust.hardwarelogger.services
import org.itsadigitaltrust.hardwarelogger.issuereporter.{Description, GitHubIssueReporter, IssueReportStatus, ReportedIssue}

private object types:
  export org.itsadigitaltrust.hardwarelogger.issuereporter.ReportedIssue as Issue
  extension (issue: ReportedIssue)
    def name: String = issue.title
export types.*


trait IssueReporterService:
  def report(issue: Issue): Option[String]  =
    report(issue.name, issue.description.value)
  def report(name: String, description: String): Option[String]

class StandardIssueReporterService extends IssueReporterService:
  import org.itsadigitaltrust.hardwarelogger.core.issueReporterDefaults
  lazy val issueReporter: GitHubIssueReporter =
    GitHubIssueReporter(null)

  override def report(name: String, description: String): Option[String] =
      issueReporter.report(ReportedIssue(name, Description(description))) match
        case IssueReportStatus.Success => None
        case IssueReportStatus.Error(message) => message


