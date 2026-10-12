package org.itsadigitaltrust.hardwarelogger.ui.controls

import scalafx.scene.control.TextFormatter.Change
import scalafx.scene.control.{TextField, TextFormatter}

import java.util.function.UnaryOperator
class ItsaIDTextField extends TextField:
  textFormatter = new TextFormatter( (change: Change) =>
    /*
    ▼ ^[a-zA-Z0-9\s.]*$ – matches elements in order
  ▶ ^ Anchor (https://www.regular-expressions.info/anchors.html) – matches before the start of the input (and after a line terminator in multi-line mode)
  ▼ [a-zA-Z0-9\s.] Character Class (https://www.regular-expressions.info/charclass.html) – matches 1 character in the set
    ▶ a-z Range (https://www.regular-expressions.info/charclass.html) – matches 1 character from LATIN SMALL LETTER A to LATIN SMALL LETTER Z (26 characters)
    ▶ A-Z Range (https://www.regular-expressions.info/charclass.html) – matches 1 character from LATIN CAPITAL LETTER A to LATIN CAPITAL LETTER Z (26 characters)
    ▶ 0-9 Range (https://www.regular-expressions.info/charclass.html) – matches 1 character from DIGIT ZERO to DIGIT NINE (10 characters)
    ▶ \s Shorthand Character Class (https://www.regular-expressions.info/shorthand.html) – matches a whitespace character
    ▶ . – matches the FULL STOP character
  ▶ * Quantifier (https://www.regular-expressions.info/repeat.html) – matches the previous element zero or more times, as many times as possible
  ▶ $ Anchor (https://www.regular-expressions.info/anchors.html) – matches after the end of the input (and before a line terminator in multi-line mode)
     */
    if change.getControlNewText.matches("^[a-zA-Z0-9\\s.]*$") then
      change
    else
      null
  )

