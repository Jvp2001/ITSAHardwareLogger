package org.itsadigitaltrust.services.validation

import org.itsadigitaltrust.common.{PropertyFileReaderError, Result, Result.Success}
import org.itsadigitaltrust.hardwarelogger.services.IDParser
import org.itsadigitaltrust.hardwarelogger.services.IDParser.{ParsedResult, ParserError}
import org.scalatest.funsuite.AnyFunSuite

class IDParserTests extends AnyFunSuite:

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
    result match
      case Result.Result.Success(value) =>
        assert(value.isInstanceOf[ParserError.ScannerError])
      case Result.Error(value) => assert(false)
    
    