package org.itsadigitaltrust.hardwarelogger.services

import org.itsadigitaltrust.common.Operators.{??, in, notIn}
import org.itsadigitaltrust.common.Result

import scala.annotation.tailrec
import scala.collection.immutable.Seq as :++
import scala.compiletime.uninitialized
import scala.util.boundary


private type Location = Int

private class Chars(val input: String):
  private var index: Int = 0

  def atEnd: Boolean =
    index >= input.length

  def peek(): Char =
    input(index)

  def nextChar(): Char =
    val char = peek()
    index += 1
    char

  def hasNext: Boolean =
    index < input.length

  def location: Location =
    index

end Chars

class IDScanner(input: String, hdMode: Boolean = false):

  import IDScanner.*


  private lazy val prefixLetters = if hdMode then hdPrefixLetters else pcPrefixLetters


  type ![T] = boundary.Label[IDScannerResult] ?=> T

  private val chars = Chars(input)
  private var nextToken: IDScannerResult = uninitialized

  def peek(): IDScannerResult =
    if nextToken eq null then
      nextToken = readToken()
    nextToken

  def next(): IDScannerResult =
    val res = peek()
    if nextToken ne Token.End then
      nextToken = null
    res

  private def readToken(): IDScannerResult =
    boundary:
      if chars.atEnd then
        Token.End
      else chars.peek() match
        case c: Char if c.isLetter =>
//          if location == 0 && !prefixLetters.contains(c.toLower) then
//            error(s"Invalid character $c")
//          else
          readLetter(Token.Letter.apply)
        case c if c.isDigit => readNumber()
        case c if c == '.' => accept('.', Token.DecimalPoint)
        case c => error(s"Unknown character: $c!")
      end if
  end readToken


  private def readLetter(token: String => Token): ![Token] =
    token(s"${readLetter()}")


  private def readLetter(): ![Char] =
    nextCharOrError()

  private def readNumber(): ![Token] =
    @tailrec
    def loop(number: String): String =
      if !chars.atEnd && chars.peek().isDigit then
        loop(s"$number${nextCharOrError()}")
      else
        number


    Token.Number(loop(""))
  //        acceptUntil(Token.Number.apply)(_.isDigit)


  private def location: Location = chars.location

  private def accept(char: Char, token: Token): ![Token] =
    nextCharOrError() match
      case `char` => token
      case next => error(s"Unexpected character: got ${chars.peek()}, but got $next")

  private def nextCharOrError(): ![Char] =
    if chars.atEnd then error("Unexpected end!")
    else chars.nextChar()


  private def error(msg: String): ![Nothing] =
    boundary.break(Token.Error(msg, chars.location))
end IDScanner

object IDScanner:
  final val pcPrefixLetters = Seq('k', 'l')
  final val hdPrefixLetters = pcPrefixLetters :++ Seq('h')
  enum Token:
    case Letter(value: String)
    case Number(value: String)
    case DecimalPoint
    case End
    case Error(msg: String, at: Location)

  type IDScannerResult = Token
end IDScanner


final class IDParser:

  import IDParser.*
  import IDScanner.Token.*
  import IDScanner.*

  type ![T] = Result.Continuation[ParsedResult, ParserError] ?=> T

  def parse(input: String, hardDriveMode: Boolean = false): ParserResult =
    Result:
      val scanner = new IDScanner(input, hardDriveMode)
      val tokens = readTokens(scanner)
      handleTokens(tokens)


  given [T]: Conversion[ParserError, ![Nothing]] with
    override def apply(x: ParserError): ![Nothing] = error(x)

  @tailrec
  private def handleTokens(tokens: Seq[Token], index: Int = 0, result: ParsedResult = ParsedResult()): ![ParsedResult] =

    if index == tokens.length then
      return result

    val token: Token = tokens(index)
    if index == 0 then
      token match
        case Token.Letter(value) =>
          if value notIn IDScanner.hdPrefixLetters then
            val validChars = IDScanner.hdPrefixLetters.map(_.toUpper).mkString(", ")
            val validStr = validChars.replace("L, ", "L or ")
            error(ParserError.InvalidCharacter(validStr, value))
          else
            handleTokens(tokens, index + 1, ParsedResult(Some(value), result.number, result.decimal, result.checkDigit, result.suffix))
        case Token.Number(value) => handleTokens(tokens, index + 1, ParsedResult(result.prefix, Some(value), result.decimal, result.checkDigit, result.suffix))
        case Token.DecimalPoint => error(ParserError.MissingNumber)
        case Token.End => result
        case error@Token.Error(msg, at) => this.error(error)
    else
      token match
        case Token.Letter(value) if index == tokens.length - 1 =>
          handleTokens(tokens, index + 1, ParsedResult(result.prefix, result.number,
            result.decimal, result.checkDigit, Some(value)))
        case Token.Letter(value) => error(ParserError.InvalidCharacter("number or a decimal point", value))
        case Token.Number(value) =>
          if result.number.isEmpty then
            handleTokens(tokens, index + 1, ParsedResult(result.prefix, Some(value), result.decimal, result.checkDigit, result.suffix))
          else
            handleTokens(tokens, index + 1, ParsedResult(result.prefix, result.number, result.decimal, Some(value), result.suffix))

        case Token.DecimalPoint =>
          if index < 2 && result.number.isEmpty then
            error(ParserError.MissingNumber)
          else if result.decimal.isDefined then
            error(ParserError.TooManyDecimalPoints)
          else
            handleTokens(tokens, index + 1, ParsedResult(result.prefix, result.number, Some("."), result.checkDigit, result.suffix))

        case Token.End => result
        case error@Token.Error(msg, at) => this.error(error)
  end handleTokens


  private def readTokens(scanner: IDScanner): ![Seq[Token]] =
    @tailrec
    def loop(tokens: Seq[Token] = Seq.empty): Seq[Token] =
      scanner.next() match
        case tokenError: Token.Error =>
          error(tokenError)
        case Token.End => tokens
        case t => loop(tokens :+ t)

    loop()
  end readTokens

  private def error(error: ParserError): ![Nothing] =
    Result.error(error)


object IDParser:
  final case class ParsedResult(prefix: Option[String] = None, number: Option[String] = None, decimal: Option[String] = None, checkDigit: Option[String] = None, suffix: Option[String] = None):
    override def toString: String =
      s"${prefix ?? ""}${number.get}${decimal ?? "."}${checkDigit ?? "0"}${suffix ?? ""}"

  type ParserResult = Result[ParsedResult, ParserError]

  enum ParserError:
    case TooShort
    case TooLong
    case MissingNumber
    case MissingCheckDigit
    case TooLongCheckDigit
    case TooManyDecimalPoints
    case InvalidCharacter(expected: String, got: String)
    case ScannerError(msg: String, at: Location)

    override def toString: String =
      this match
        case TooShort => "ID is too short in length."
        case TooLong => "ID is too long in length."
        case MissingNumber => "Missing the number part of the ID before the decimal point."
        case MissingCheckDigit => "ID is missing the check digit"
        case TooLongCheckDigit => "The check digit should be a single digit"
        case TooManyDecimalPoints => "You must only have one decimal point in the ID."
        case InvalidCharacter(expected, got) => s"Expected $expected, but got $got instead."
        case ScannerError(msg, at) => s"Scanner error: $msg at location $at."
  end ParserError

  given [T]: Conversion[ParserError, ParserResult] with
    override def apply(x: ParserError): ParserResult =
      Result:
        Result.error(x)

  given Conversion[IDScanner.Token.Error, ParserError] with
    override def apply(x: IDScanner.Token.Error): ParserError =
      ParserError.ScannerError(x.msg, x.at)

  given [E]: Conversion[ParsedResult, ParserResult] with
    def apply(x: ParsedResult): ParserResult =
      Result:
        Result.success(x)


  def apply(input: String, hardDriveMode: Boolean = false): ParserResult =
    val parser = new IDParser
    parser.parse(input, hardDriveMode)
end IDParser