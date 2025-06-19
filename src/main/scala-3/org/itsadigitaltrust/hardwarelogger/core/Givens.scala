package org.itsadigitaltrust.hardwarelogger.core

import org.itsadigitaltrust.common.{PropertyFileReader, Result}
import org.itsadigitaltrust.common.processes.ProcessConfig
import org.itsadigitaltrust.hardwarelogger.issuereporter.IssueReporterProps


given issueReporterDefaults: IssueReporterProps = new IssueReporterProps(/*945572141L*/ 0L, "Jvp2001", "") //"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")
given HLProcessConfig: ProcessConfig = new ProcessConfig:
  override lazy val sudoPassword: String = "itsa"