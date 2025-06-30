package org.itsadigitaltrust.hardwarelogger.core

import org.itsadigitaltrust.common.{PropertyFileReader, Result}
import org.itsadigitaltrust.common.processes.ProcessConfig
import org.itsadigitaltrust.hardwarelogger.issuereporter.IssueReporterProps


given issueReporterDefaults: IssueReporterProps = new IssueReporterProps(/*945572141L*/ 0L, "Jvp2001", "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") //"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")
given HLProcessConfig: ProcessConfig = new ProcessConfig:
  override lazy val sudoPassword: String = "itsa"