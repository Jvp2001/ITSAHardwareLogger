package org.itsadigitaltrust.hardwarelogger.issuereporter

opaque type Description = String

object Description:
  def apply(value: String): Description = value

  extension (desc: Description)
    def value: String = desc
final case class ReportedIssue(title: String, description: Description)
object ReportedIssue:
  def apply(): ReportedIssue = new ReportedIssue("", Description(""))
