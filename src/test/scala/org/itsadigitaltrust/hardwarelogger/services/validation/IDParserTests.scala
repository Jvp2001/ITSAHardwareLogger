package org.itsadigitaltrust.hardwarelogger.services.validation

import org.itsadigitaltrust.hardwarelogger.services.IDParser
import org.itsadigitaltrust.hardwarelogger.services.IDParser.{ParsedResult, ParserError}

import org.scalatest.funsuite.AnyFunSuite

class IDParserTests extends AnyFunSuite:


  private def isInvalidCharacterAt(result: Either[ParserError, ParsedResult], index: Int): Boolean =
    result match
      case Left(value: ParserError.InvalidCharacter) => index == value.at
      case Right(value) => false


  test("Valid ID with prefix and suffix"):
    val result = IDParser("H82437.1A")
    assert(result == Right(ParsedResult(Some("H"), Some("82437"), Some("."), Some("1"), Some("A"))))

  test("Valid ID with prefix only"):
    val result = IDParser("H82437.1")
    assert(result == Right(ParsedResult(Some("H"), Some("82437"), Some("."), Some("1"), None)))

  test("Valid ID with suffix only"):
    val result = IDParser("82437.1A")
    assert(result == Right(ParsedResult(None, Some("82437"), Some("."), Some("1"), Some("A"))))

  test("Valid ID without prefix and suffix"):
    val result = IDParser("82437.1")
    assert(result == Right(ParsedResult(None, Some("82437"), Some("."), Some("1"), None)))


  test("Invalid ID with too many decimal points"):
    val result = IDParser("H82437.1.2")
    assert(result == Left(ParserError.TooManyDecimalPoints))

  test("Invalid ID with missing number"):
    val result = IDParser("H.1A")
    assert(result == Left(ParserError.MissingNumber))

  test("Invalid ID with invalid character"):
    val result = IDParser("H82437.1@")
    assert(isInvalidCharacterAt(result, 8))

  test("Invalid ID with invalid character at 3"):
    val result = IDParser("H82@37.1@", true)
    assert(isInvalidCharacterAt(result, 3))


  test("Valid ID with prefix and suffix in hard drive mode"):
    val result = IDParser("H82437.1A", true)
    assert(result == Right(ParsedResult(Some("H"), Some("82437"), Some("."), Some("1"), Some("A"))))

  test("Valid ID with prefix only in hard drive mode"):
    val result = IDParser("H82437.1", true)
    assert(result == Right(ParsedResult(Some("H"), Some("82437"), Some("."), Some("1"), None)))

  test("Valid ID with suffix only in hard drive mode"):
    val result = IDParser("82437.1A", true)
    assert(result == Right(ParsedResult(None, Some("82437"), Some("."), Some("1"), Some("A"))))

  test("Valid ID without prefix and suffix in hard drive mode"):
    val result = IDParser("82437.1", true)
    assert(result == Right(ParsedResult(None, Some("82437"), Some("."), Some("1"), None)))

  test("Invalid ID with too many decimal points in hard drive mode"):
    val result = IDParser("H82437.1.2", true)
    assert(result == Left(ParserError.TooManyDecimalPoints))

  test("Invalid ID with missing number in hard drive mode"):
    val result = IDParser("H.1A", true)
    assert(result == Left(ParserError.MissingNumber))

  test("Invalid ID with invalid character in hard drive mode"):
    val result = IDParser("H82437.1@", true)
    assert(isInvalidCharacterAt(result, 8))

  test("Invalid ID with invalid character at 3 in hard drive mode"):
    val result = IDParser("H82@37.1@", true)
    assert(isInvalidCharacterAt(result, 3))

end IDParserTests

object IDParserTests:
  @main
  def main(): Unit =
    val result = IDParser("H82437.1@")
    println(result)