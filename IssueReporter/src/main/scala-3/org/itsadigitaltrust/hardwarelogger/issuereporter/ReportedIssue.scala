package org.itsadigitaltrust.hardwarelogger.issuereporter

opaque type Description = String

object Description:
  def apply(value: String): Description = value

  extension (desc: Description)
    def value: String = desc
final case class ReportedIssue private(title: String, description: Description)
object ReportedIssue:
  def apply(title: String = "Issue", description: Description = Description("")): ReportedIssue = new ReportedIssue(title, description)
