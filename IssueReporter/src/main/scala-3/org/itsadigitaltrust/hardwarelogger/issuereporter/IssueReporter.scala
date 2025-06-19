package org.itsadigitaltrust.hardwarelogger.issuereporter

import org.itsadigitaltrust.common.Operators.{??, |>}
import org.itsadigitaltrust.common.{PropertyFileReader, Result, optional}
import org.kohsuke.github.GitHubBuilder

import java.io.IOException
import java.util.Properties
import scala.util.{Failure, Success, Try}


enum IssueReportStatus:
  case Success
  case Error(message: Option[String])

trait IssueReporter:
  import IssueReporter.!


  def report(issue: ReportedIssue): IssueReportStatus

object IssueReporter:

  import org.itsadigitaltrust.common.optional

  type ![T] = optional.Label[T] ?=> T

final class GitHubIssueReporter(fileName: String)(using default: IssueReporterProps ) extends IssueReporter:
  import scala.jdk.CollectionConverters.*
  import IssueReporter.!

  val propsFile = Option[String](fileName)
  private val propsFileReader = PropertyFileReader.from(propsFile ?? "").success
  private val props = propsFileReader.props ?? new Properties(3):
    putAll(Map("repo" -> default.repoID.toString, "login" -> default.accountName, "oauth" -> default.token).asJava)



  private val github = GitHubBuilder.fromProperties(props).build()
  override def report(issue: ReportedIssue) : IssueReportStatus =
    val id = props.getProperty("repo").toLongOption ?? default.repoID
    val gitIssue  = github.getRepositoryById(id).createIssue(issue.title ?? "")
    gitIssue.label("Bug")
    gitIssue.body(issue.description.value ?? "")
     Try(gitIssue.create()) match
      case Failure(exception) => IssueReportStatus.Error(exception.getMessage |> Option[String])
      case Success(_) => IssueReportStatus.Success
  end report

end GitHubIssueReporter






