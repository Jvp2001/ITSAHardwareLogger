package org.itsadigitaltrust.services.validation

import org.itsadigitaltrust.hardwarelogger.services.IDScanner
import org.scalatest.funsuite.AnyFunSuite


import scala.annotation.tailrec

class IDScannerTests extends AnyFunSuite:

  import IDScanner.Token

  private def readTokens(input: String): Seq[IDScanner.Token] =
    val scanner = new IDScanner(input)

    @tailrec
    def loop(tokens: Seq[IDScanner.Token]): Seq[IDScanner.Token] =
      scanner.next() match
        case Token.Error(msg, at) =>
          assert(false, s"Error at $at: $msg")
          Seq()
        case Token.End => tokens
        case t => loop(tokens :+ t)

    loop(Seq.empty)


  test("Valid 4 4 Digit ID"):
    import Token.*
    val tokens = readTokens("H8247.1A")
    assert(tokens == Seq(Letter("H"), Number("8247"), DecimalPoint, Number("1"), Letter("A")))

  test("Valid 4 No Prefix"):
    import Token.*
    val tokens = readTokens("8247.1A")
    assert(tokens == Seq(Number("8247"), DecimalPoint, Number("1"), Letter("A")))

  test("Valid 4 No Suffix"):
    import Token.*
    val tokens = readTokens("H8247.1")
    assert(tokens == Seq(Letter("H"), Number("8247"), DecimalPoint, Number("1")))

  test("Valid 4 No Prefix and No Suffix"):
    import Token.*
    val tokens = readTokens("8247.1")
    assert(tokens == Seq(Number("8247"), DecimalPoint, Number("1")))


  test("Valid 5 Digit ID"):
    import Token.*
    val tokens = readTokens("H82437.1A")
    assert(tokens == Seq(Letter("H"), Number("82437"), DecimalPoint, Number("1"), Letter("A")))

  test("Valid 5 No Prefix"):
    import Token.*
    val tokens = readTokens("82437.1A")
    assert(tokens == Seq(Number("82437"), DecimalPoint, Number("1"), Letter("A")))

  test("Valid 5 No Suffix"):
    import Token.*
    val tokens = readTokens("H82437.1")
    assert(tokens == Seq(Letter("H"), Number("82437"), DecimalPoint, Number("1")))

  test("Valid 5 No Prefix and No Suffix"):
    import Token.*
    val tokens = readTokens("82437.1")
    assert(tokens == Seq(Number("82437"), DecimalPoint, Number("1")))