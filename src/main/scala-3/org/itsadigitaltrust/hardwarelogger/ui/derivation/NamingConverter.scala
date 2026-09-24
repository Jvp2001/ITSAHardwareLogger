package org.itsadigitaltrust.hardwarelogger.ui.derivation

extension(s: String)


  // New method: Adds a space before any uppercase letter (unless it's already preceded by a space)
  def spaceBeforeUppercase: String =
    "(?<=.)(?=\\p{Lu})".r.replaceAllIn(s, " ")

  // Modified from https://github.com/kavedaa/kavedaa.github.io/blob/master/auto-ui-generation/src/main/scala/util.scala
  // New & Repaired version: Correctly inserts spaces and avoids duplicating index 0 letters
  def fromCamelCase: String =
    if s.isEmpty then ""
    else
      val sb = StringBuilder()
      // Always capitalize the first character safely
      sb.append(s.head.toUpper)

      // Process the remaining characters safely
      s.tail.zipWithIndex.foreach: (curr, idx) =>
        val actualIdx = idx + 1
        val prev = s(actualIdx - 1)

        // Add a space if it's a boundary (lowercase followed by uppercase, or uppercase before lowercase like an acronym end)
        val isLowerToUpper = prev.isLower && curr.isUpper
        val isAcronymEnd = actualIdx + 1 < s.length && prev.isUpper && curr.isUpper && s(actualIdx + 1).isLower

        if isLowerToUpper || isAcronymEnd then
          sb.append(' ')

        sb.append(curr)

      // Capitalize the first letter of words while leaving the rest intact, or make standard updates
      sb.toString

  def toCamelCase: String =
    val sb = StringBuilder()
    s.zipWithIndex.foreach: (char, index) =>
      if char == s(0) then
        sb += char.toLower
      else if char.isLetter && s(index - 1) == ' ' then
        sb += char.toUpper
      else if !" -_".contains(char) then
        sb += char
    sb.toString
  end toCamelCase
end extension


@main
def NamingConvertersTesting: Unit =
  println:
    "meaningOfLife".fromCamelCase.toCamelCase

