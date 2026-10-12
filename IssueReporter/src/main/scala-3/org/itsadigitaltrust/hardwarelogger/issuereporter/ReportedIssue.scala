package org.itsadigitaltrust.hardwarelogger.issuereporter


trait IssuePayload

final case class GitHubIssuePayload(
                                   title: String,
                                   body: String,
                                   labels: List[String],
                                   severity: String
                                   ) extends IssuePayload
trait IssueTransformer[A, P <: IssuePayload]:
  def toPayload(issue: A): P
  final def apply(issue: A): P = 
    toPayload(issue)

type GitHubIssueTransformer[A] = IssueTransformer[A, GitHubIssuePayload]



final case class ReportedIssue private(title: String, body: String)
object ReportedIssue:
  def apply(title: String = "Issue", description: String = String("")): ReportedIssue = new ReportedIssue(title, description)
