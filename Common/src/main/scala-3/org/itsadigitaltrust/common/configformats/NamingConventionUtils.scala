package org.itsadigitaltrust.common.configformats

import java.util.regex.Pattern

object NamingConventionUtils:

  private val kebabPattern = Pattern.compile("-[a-zA-Z]")

  /**
   * Converts a string from camelCase (or PascalCase) to kebab-case.
   * Example: "unit7Address" -> "unit7-address"
   */
  def camelToKebab(input: String): String =
    if input == null || input.isEmpty then ""
    else
      input
        .replaceAll("(?<=^[a-z0-9])([A-Z])", "-$1")
        .replaceAll("(?<=[a-zA-Z0-9])([A-Z])(?=[a-z])", "-$1")
        .toLowerCase

  /**
   * Converts a string from kebab-case to camelCase.
   * Example: "unit7-address" -> "unit7Address"
   */
  def kebabToCamel(input: String): String =
    if input == null || input.isEmpty then ""
    else
      val matcher = kebabPattern.matcher(input)
      // FIX: Explicitly type as java.lang.StringBuilder to match Java's Regex API signature
      val sb = new java.lang.StringBuilder()
      while matcher.find() do
        matcher.appendReplacement(sb, matcher.group().substring(1).toUpperCase)
      matcher.appendTail(sb)
      sb.toString
