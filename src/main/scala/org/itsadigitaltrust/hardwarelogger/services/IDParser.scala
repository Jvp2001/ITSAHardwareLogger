package org.itsadigitaltrust.hardwarelogger.services


import scala.annotation.tailrec
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

class IDScanner(input: String):

  import IDScanner.*

  private var chars = Chars(input)
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
        case c: Char if c.isLetter => readLetter(Token.Letter)
        case c if c.isDigit => readNumber()
        case c if c == '.' => accept('.', Token.DecimalPoint)
        case c => error(s"Unknown character: $c!")


  private def readLetter(token: String => Token)(using boundary.Label[IDScannerResult]): Token =
    token(s"${readLetter()}")


  private def readLetter()(using boundary.Label[IDScannerResult]): Char =
    nextCharOrError()

  private def readNumber()(using boundary.Label[IDScannerResult]): Token =
    @tailrec
    def loop(number: String): String =
      if !chars.atEnd && chars.peek().isDigit then
        loop(s"$number${nextCharOrError()}")
      else
        number

    Token.Number(loop(""))
  //        acceptUntil(Token.Number.apply)(_.isDigit)


  private def accept(char: Char, token: Token)(using boundary.Label[IDScannerResult]): Token =
    nextCharOrError() match
      case `char` => token
      case next => error(s"Unexpected character: got ${chars.peek()}, but got $next")

  private def nextCharOrError()(using boundary.Label[IDScannerResult]): Char =
    if chars.atEnd then error("Unexpected end!")
    else chars.nextChar()


  private def error(msg: String)(using boundary.Label[IDScannerResult]): Nothing =
    boundary.break(Token.Error(msg, chars.location))
end IDScanner

object IDScanner:

  enum Token:
    case Letter(value: String)
    case Number(value: String)
    case DecimalPoint
    case End
    case Error(msg: String, at: Location)

  type IDScannerResult = Token
end IDScanner


class IDParser:

  import IDParser.*
  import IDScanner.*
  import IDScanner.Token.*

  def parse(input: String): ParserResult =
    boundary:
      val scanner = new IDScanner(input)
      val tokens = readTokens(scanner)
      handleTokens(tokens)


  @tailrec
  private def handleTokens(tokens: Seq[Token], index: Int = 0, result: ParsedResult = ParsedResult())(using boundary.Label[ParserResult]): ParserResult =

    if index == tokens.length then
      return result

    val token: Token = tokens(index)
    if index == 0 then
      token match
        case Token.Letter(value) => handleTokens(tokens, index + 1, ParsedResult(Some(value), result.number, result.decimal, result.checkDigit, result.suffix))
        case Token.Number(value) => handleTokens(tokens, index + 1, ParsedResult(result.prefix, Some(value), result.decimal, result.checkDigit, result.suffix))
        case Token.DecimalPoint => ParserError.MissingNumber
        case Token.End => result
        case error@Token.Error(msg, at) => Left(error)
    else
      token match
        case Token.Letter(value) if index == tokens.length - 1 =>
          handleTokens(tokens, index + 1, ParsedResult(result.prefix, result.number,
            result.decimal, result.checkDigit, Some(value)))
        case Token.Letter(value) => ParserError.InvalidCharacter("number or a decimal point", value)
        case Token.Number(value) =>
          if result.number.isEmpty then
            handleTokens(tokens, index + 1, ParsedResult(result.prefix, Some(value), result.decimal, result.checkDigit, result.suffix))
          else
            handleTokens(tokens, index + 1, ParsedResult(result.prefix, result.number, result.decimal, Some(value), result.suffix))

        case Token.DecimalPoint =>
          if index < 2 && result.number.isEmpty then
            ParserError.MissingNumber
          else if result.decimal.isDefined then
            ParserError.TooManyDecimalPoints
          else
            handleTokens(tokens, index + 1, ParsedResult(result.prefix, result.number, Some("."), result.checkDigit, result.suffix))

        case Token.End => result
        case error@Token.Error(msg, at) => Left(error)
  end handleTokens


  private def readTokens(scanner: IDScanner)(using boundary.Label[ParserResult]): Seq[Token] =
    @tailrec
    def loop(tokens: Seq[Token] = Seq.empty): Seq[Token] =
      scanner.next() match
        case tokenError: Token.Error =>
          error(tokenError)
        case Token.End => tokens
        case t => loop(tokens :+ t)

    loop()
  end readTokens

  private def error(error: ParserError)(using boundary.Label[ParserResult]): Nothing =
    boundary.break(Left(error))


object IDParser:
  final case class ParsedResult(prefix: Option[String] = None, number: Option[String] = None, decimal: Option[String] = None, checkDigit: Option[String] = None, suffix: Option[String] = None)

  private type ParserResult = Either[ParserError, ParsedResult]

  enum ParserError:
    case TooManyLetters
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
        case TooManyLetters => "The ID must start with H, L or K."
        case TooShort => "ID is too short in length."
        case TooLong => "ID is too long in length."
        case MissingNumber => "Missing the number part of the ID before the decimal point."
        case MissingCheckDigit => "ID is missing the check digit"
        case TooLongCheckDigit => "The check digit should be a single digit"
        case TooManyDecimalPoints => "You must only have one decimal point in the ID."
        case InvalidCharacter(expected, got) => s"Expected $expected, but got $got   instead."
        case ScannerError(msg, at) => s"Scanner error: $msg at location $at."
  end ParserError

  given Conversion[ParserError, ParserResult] with
    override def apply(x: ParserError): ParserResult = Left(x)

  given Conversion[IDScanner.Token.Error, ParserError] with
    override def apply(x: IDScanner.Token.Error): ParserError = ParserError.ScannerError(x.msg, x.at)

  given Conversion[ParsedResult, ParserResult] with
    def apply(x: ParsedResult): ParserResult = Right(x)

  def apply(input: String): ParserResult =
    val parser = new IDParser
    parser.parse(input)
end IDParser