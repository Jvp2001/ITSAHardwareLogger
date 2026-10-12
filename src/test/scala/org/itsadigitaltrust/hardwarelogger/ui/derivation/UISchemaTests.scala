package org.itsadigitaltrust.hardwarelogger.ui.derivation

import org.itsadigitaltrust.hardwarelogger.services.{Issue, IssueSeverity}
import org.itsadigitaltrust.hardwarelogger.ui.derivation.CustomDataTypeSchemaGivens.given
import org.itsadigitaltrust.hardwarelogger.issuereporter.GitHubIssueReporter
import org.itsadigitaltrust.common.ConfigsManager
import org.scalatest.funsuite.AnyFunSuite

import java.nio.file.Files
import java.util.UUID

class UISchemaTests extends AnyFunSuite:
  test("BugReport derivation reconstructs its typed fields"):
    val initialReport =
      Issue.BugReport("Report title", "Report body", "diagnostic logs", IssueSeverity.High)
    val (fields, reconstruct) = summon[UISchema[Issue.BugReport]](initialReport)

    assert(fields.map(_.label) == List("title", "body"))
    assert(reconstruct() == initialReport)

  test("enum lists have a CheckComboBox field factory"):
    assert(summon[UIControlFactory[List[IssueSeverity]]] != null)

  test("GitHub reporter loads connection properties from its config file"):
    val configPath = Files.createTempFile("github-issue-reporter", ".properties")
    try
      Files.writeString(configPath, "repo=123\nlogin=reporter\noauth=not-a-real-token\n")
      assert(GitHubIssueReporter(configPath.toString).isRight)
    finally
      Files.deleteIfExists(configPath)

  test("GitHub reporter identifies a missing repository setting and config path"):
    val configPath = Files.createTempFile("github-issue-reporter", ".properties")
    try
      Files.writeString(configPath, "login=reporter\noauth=not-a-real-token\n")
      val result = GitHubIssueReporter(configPath.toString)

      assert(result.left.exists(error =>
        error.getMessage.contains("Missing required property: repo") &&
          error.getMessage.contains(configPath.toString)
      ))
    finally
      Files.deleteIfExists(configPath)

  test("default config creation preserves an existing user config"):
    val fileName = s"config-preservation-${UUID.randomUUID()}.properties"
    val firstContents = "setting=user value\n"
    val defaultContents = "setting=default value\n"
    val configPath = ConfigsManager.writeConfigIfMissing(fileName, firstContents).get
    try
      ConfigsManager.writeConfigIfMissing(fileName, defaultContents).get
      assert(Files.readString(configPath) == firstContents)
    finally
      Files.deleteIfExists(configPath)
