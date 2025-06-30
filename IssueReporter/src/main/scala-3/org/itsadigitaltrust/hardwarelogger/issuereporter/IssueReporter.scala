package org.itsadigitaltrust.hardwarelogger.issuereporter

import org.itsadigitaltrust.common.Operators.{??, |>}
import org.itsadigitaltrust.common.{PropertyFileReader, Result}
import org.kohsuke.github.GitHubBuilder

import java.io.IOException
import java.net.URI
import java.util.Properties
import scala.util.{Failure, Success, Try}


enum IssueReportStatus:
  case Success
  case Error(message: Option[String])

trait IssueReporter:


  def report(issue: ReportedIssue): IssueReportStatus



final class GitHubIssueReporter(file: Try[String])(using default: IssueReporterProps) extends IssueReporter:



  private val propsFileReader = PropertyFileReader(file).success
  private lazy val props =
    val p = new Properties()
    p.setProperty("repo", "945572141")
    p.setProperty("login", "jvp2001")
    p.setProperty("oauth", "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")
    p
  end props

  private val github = GitHubBuilder.fromProperties(props).build()

  override def report(issue: ReportedIssue): IssueReportStatus =
    val id = props.getProperty("repo").toLongOption ?? default.repoID
    val gitIssue = github.getRepositoryById(id).createIssue(issue.title ?? "")
    gitIssue.label("Bug")
    gitIssue.body(issue.description.value ?? "")
    Try(gitIssue.create()) match
      case Failure(exception) => IssueReportStatus.Error(exception.getMessage |> Option[String])
      case Success(_) => IssueReportStatus.Success
  end report

end GitHubIssueReporter






