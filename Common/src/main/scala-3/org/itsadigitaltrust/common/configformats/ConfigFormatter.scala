package org.itsadigitaltrust.common.configformats

import ox.pipe
trait ConfigFormatter:
  def format(configInfo: CaseClassConfigInfo, indentSpaces: Int): String
end ConfigFormatter

object ConfigFormatter:
  /** Standard Java Properties file specification implementation */
  object JavaProperties extends ConfigFormatter:
    def format(configInfo: CaseClassConfigInfo, indentSpaces: Int): String =
      val stringBuilder = new StringBuilder()
      val prefixGroup = configInfo.groupPrefix
      val padding = " ".repeat(indentSpaces)

      val fields = configInfo.fields.map: field =>
        val finalizedKey =
          if field.keyName.trim != "" then
            if field.keyName == field.name then
              field.keyName pipe NamingConventionUtils.camelToKebab
            else
              field.keyName
          else if prefixGroup != "" then
            s"$prefixGroup.${field.name  pipe NamingConventionUtils.camelToKebab}"
          else
            field.name pipe NamingConventionUtils.camelToKebab

        field.copy(keyName = finalizedKey)
      end fields

      fields.foreach: field =>
        // FIX: If comment is blank, inject fallback label explicitly on its own line ABOVE the property
        val rawComment =
          if field.comment.trim != "" then
            field.comment.trim
          else
            s"Property parameter: ${field.name}"

        val cleanComment = rawComment
          .replaceAll("""/\*\*+""", "")
          .replaceAll("""\*+/""", "")
          .replaceAll("(?m)^\\s*\\*\\s?", "")
          .trim

        cleanComment.split("\n").foreach: line =>
          stringBuilder.append(s"$padding# ${line.trim}\n")

        // 2. Escape Java property keys and multiline values
        val escapedKey = field.keyName
          .replace(" ", "\\ ")
          .replace("=", "\\=")
          .replace(":", "\\:")

        val escapedValue = field.value
          .replace("\n", "\\\n")

        // 3. Generate the canonical property statement
        stringBuilder.append(s"$padding$escapedKey=$escapedValue\n")

      stringBuilder.toString
    end format
  end JavaProperties
end ConfigFormatter
