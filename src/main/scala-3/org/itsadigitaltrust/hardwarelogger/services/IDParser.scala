package org.itsadigitaltrust.hardwarelogger.services

import org.itsadigitaltrust.common.Operators.*
import org.itsadigitaltrust.common.Result

import org.itsadigitaltrust.hardwarelogger.services.IDParser.ParserError.ScannerError
import org.itsadigitaltrust.hardwarelogger.services.IDScanner.Token

import ox.either.{fail, ok}

import scala.annotation.tailrec
import scala.collection.immutable.Seq as :++
import scala.compiletime.uninitialized
import scala.util.boundary
import scala.util.boundary.Label


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
  private var nextToken: Option[IDScannerResult]  = None

  def peek(): IDScannerResult =
    if nextToken eq None then
      nextToken = Option(readToken())
    nextToken.orNull

  def next(): IDScannerResult =
    val res = peek()
    if nextToken ne Token.End then
      nextToken = None
    res

  private def readToken(): IDScannerResult =
    boundary:
      if chars.atEnd then
        boundary.break(Token.End)
      else chars.peek() match
        case c: Char if c.isLetter =>
          //          if location == 0 && !prefixLetters.contains(c.toLower) then
          //            error(s"Invalid character $c")
          //          else
          readLetter(Token.Letter.apply)
        case c if c.isDigit => readNumber()
        case c if c == '.' => accept('.', Token.DecimalPoint)
        case c => error(IDScanner.ErrorCode.UnexpectedCharacter, s"Unknown character: $c!")
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
      case next => error(IDScanner.ErrorCode.UnexpectedCharacter, s"Unexpected character: got ${chars.peek()}, but got $next")

  private def nextCharOrError(): ![Char] =
    if chars.atEnd then error(IDScanner.ErrorCode.UnexpectedEnd, "Unexpected end!")
    else chars.nextChar()


  private def error(code: IDScanner.ErrorCode, msg: String): ![Nothing] =
    boundary.break(Token.Error(code, msg, chars.location))
end IDScanner

object IDScanner:
  final val pcPrefixLetters = Seq('k', 'l')
  final val hdPrefixLetters = pcPrefixLetters :++ Seq('h')

  enum ErrorCode:
    case UnexpectedCharacter
    case UnexpectedEnd
    case Unknown
  enum Token:
    case Letter(value: String)
    case Number(value: String)
    case DecimalPoint
    case End
    case Error(code: ErrorCode, msg: String, at: Location)

  type IDScannerResult = Token
end IDScanner


final class IDParser:

  import IDParser.*
  import IDScanner.Token.*
  import IDScanner.*

  import org.itsadigitaltrust.hardwarelogger.services.IDParser.
  given_Conversion_Error_ParserError

  type ![T] = Label[Either[ParserError, T]] ?=> T
  private type ContextLabel[T] = Label[Either[ParserError, T]]

  def parse(input: String, hardDriveMode: Boolean = false): ParserResult =
    ox.either:
      val scanner = new IDScanner(input, hardDriveMode)
      val tokens: Seq[Token] = readTokens(scanner).ok()
      handleTokens(tokens)

  @tailrec
  private def handleTokens(tokens: Seq[Token], index: Int = 0, result: ParsedResult = ParsedResult())(using label: ContextLabel[ParsedResult]): ![ParsedResult] =

    if index == tokens.length then
      return result

    val token: Token = tokens(index)
    if index == 0 then
      token match
        case Token.Letter(value) =>
          if value.toLowerCase |> IDScanner.hdPrefixLetters.contains then
            val validChars = IDScanner.hdPrefixLetters.map(_.toUpper).mkString(", ")
            val validStr = validChars.replace("L, ", "L or ")
             ParserError.InvalidCharacter(validStr, value, index + 1).fail()
          else
            handleTokens(tokens, index + 1, result.copy(Some(value)))
        case Token.Number(value) => handleTokens(tokens, index + 1, result.copy(number = Some(value)))
        case Token.DecimalPoint => ParserError.MissingNumber.fail()
        case Token.End => result
        case error@Token.Error(_, _, _) => error.toParserError.fail()
    else
      token match
        case Token.Letter(value) if index == tokens.length - 2 => // Last token before End
          handleTokens(tokens, index + 1, result.copy(suffix = Some(value)))
        case Token.Letter(value) =>  ParserError.InvalidCharacter("number or a decimal point", value, index+1).fail()
        case Token.Number(value) =>
          if result.number.isEmpty then
            handleTokens(tokens, index + 1, result.copy(number = Some(value)))
          else
            handleTokens(tokens, index + 1, result.copy(checkDigit = Some(value)))

        case Token.DecimalPoint =>
          print("found decimal point")
          if index < 2 && result.number.isEmpty then
            ParserError.MissingNumber.fail()
          else if result.decimal.isDefined then
            ParserError.TooManyDecimalPoints.fail()
          else
            handleTokens(tokens, index + 1, result.copy(decimal = Some(".")))

        case Token.End =>
          if result.decimal.isEmpty then
            ParserError.MissingCheckDigit.fail()
          else if result.checkDigit.isEmpty then
            ParserError.MissingCheckDigit.fail()
          result
        case error@Token.Error(_, _, _) => error.toParserError.fail()
  end handleTokens


  private def readTokens(scanner: IDScanner)(using label: ContextLabel[ParsedResult]): Either[ParserError, Seq[Token]] =
    @tailrec
    def loop(tokens: Seq[Token] = Seq.empty): Either[ParserError, Seq[Token]] =
      scanner.next() match
        case tokenError: Token.Error =>
           Left(tokenError)
        case Token.End => Right(tokens :+ Token.End) //So I can perform post-validation checks.
        case t => loop(tokens :+ t)

    loop()
  end readTokens


object IDParser:
  final case class ParsedResult(prefix: Option[String] = None, number: Option[String] = None, decimal: Option[String] = None, checkDigit: Option[String] = None, suffix: Option[String] = None):
    override def toString: String =
      s"${prefix ?? ""}${number.get}${decimal ?? ""}${checkDigit ?? ""}${suffix ?? ""}"


  extension(tokenError: Token.Error)
    def toParserError: ParserError =
      tokenError.code match
        case IDScanner.ErrorCode.UnexpectedCharacter => ParserError.InvalidCharacter(expected = "A letter or a digit", got = tokenError.msg, at = tokenError.at)
        case IDScanner.ErrorCode.UnexpectedEnd => ParserError.TooShort
        case IDScanner.ErrorCode.Unknown => ParserError.ScannerError(tokenError.msg, tokenError.at)

  type ParserResult = Either[ParserError, ParsedResult]
  final class ParserException(val error: ParserError) extends HLFrontendException(error.toString)
  enum ParserError:
    case TooShort
    case TooLong
    case MissingNumber
    case MissingCheckDigit
    case TooLongCheckDigit
    case MissingDecimalPoint
    case TooManyDecimalPoints
    case InvalidCharacter(expected: String, got: String, at: Location)
    case ScannerError(msg: String, at: Location)


    def toParserException: ParserException = ParserException(this)
    override def toString: String =
      this match
        case TooShort => "ID is too short in length."
        case TooLong => "ID is too long in length."
        case MissingNumber => "Missing the number part of the ID before the decimal point."
        case MissingCheckDigit => "ID is missing the check digit"
        case TooLongCheckDigit => "The check digit should be a single digit"
        case TooManyDecimalPoints => "You must only have one decimal point in the ID."
        case MissingDecimalPoint => "ID is missing the decimal point."
        case InvalidCharacter(expected, got, at) => s"Expected $expected, but got $got instead at location $at."
        case ScannerError(msg, at) => s"Scanner error: $msg at location $at."
  end ParserError

  given [T]: Conversion[ParserError, ParserResult] with
    override def apply(x: ParserError): ParserResult =
      Left(x)

  given Conversion[IDScanner.Token.Error, ParserError] with
    override def apply(x: IDScanner.Token.Error): ParserError =
      x.toParserError

  given [E]: Conversion[ParsedResult, ParserResult] with
    def apply(x: ParsedResult): ParserResult =
      Right(x)


  def apply(input: String, hardDriveMode: Boolean = false): ParserResult =
    val parser = new IDParser
    parser.parse(input, hardDriveMode)
end IDParser