package org.itsadigitaltrust.hardwarelogger.issuereporter

import org.itsadigitaltrust.common.configformats.{ConfigKey, JavaPropertiesConverter, JavaPropertiesReader}
import org.kohsuke.github.GitHubBuilder

import java.nio.file.Paths
import scala.Conversion.into
import scala.util.Try


enum IssueReportStatus:
  case Success
  case Error(message: Option[String])

case class IssueReportConnectionProps(
                                      repo: Long,
                                      @ConfigKey("login")
                                      userName: String,
                                      oauth: String
                                    )

trait IssueReporter[P <: IssuePayload](using val props: IssueReportConnectionProps):
  def report[A](issue: A)(using transformer: IssueTransformer[A, P]): IssueReportStatus

final class GitHubIssueReporter(using IssueReportConnectionProps) extends IssueReporter[GitHubIssuePayload]:
  private val github = GitHubBuilder.fromProperties(JavaPropertiesConverter(props)).build()

  override def report[A](issue: A)(using transformer: IssueTransformer[A, GitHubIssuePayload]): IssueReportStatus =
    Try:
      val instance = transformer(issue)
      val gitIssue = github.getRepositoryById(props.repo).createIssue(instance.title)
      gitIssue.body(instance.body)
      instance.labels.foreach(label => gitIssue.label(label))
      gitIssue.create()
    .fold(
      ex =>
        ex match
          case httpError: org.kohsuke.github.HttpException if httpError.getResponseCode == 401 =>
            IssueReportStatus.Error(Some(
              "GitHub rejected the configured OAuth token (401). Replace oauth in Configs/issuereporter.properties with a valid token and restart the application."
            ))
          case other =>
            IssueReportStatus.Error(Option(other.getMessage).orElse(Some(other.toString))),
      _ => IssueReportStatus.Success
    )

  end report

end GitHubIssueReporter

object GitHubIssueReporter:
  def apply(configFile: into[String]): Either[Throwable, GitHubIssueReporter] =
    for
      props <- JavaPropertiesReader
        .readFile[IssueReportConnectionProps](Paths.get(configFile.toString))
        .left.map(error => RuntimeException(s"Invalid GitHub issue reporter config at '$configFile': $error"))
      reporter <- Try(new GitHubIssueReporter(using props)).toEither
    yield reporter

end GitHubIssueReporter
