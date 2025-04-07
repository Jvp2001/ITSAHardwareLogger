package org.itsadigitaltrust.hardwarelogger.services


import java.util.regex.Pattern
import scala.annotation.tailrec
import scala.util.boundary
import org.itsadigitaltrust.common.*
import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.services.IDParser.ParsedResult


trait HardwareIDValidationService:

  import HardwareIDValidationService.*

  def validate(input: String): ValidationResult

object HardwareIDValidationService:
  type ValidationResult = Result[ParsedResult, ValidationError]

  enum ValidationError:
    case ParserError(error: IDParser.ParserError)
    case IncorrectCheckDigit(expected: String, got: String)

    override def toString: String =
      this match
        case ParserError(error) => error.toString()
        case IncorrectCheckDigit(expected, got) => s"Not valid ID. Check if the ID is correct on the device."

  given Conversion[IDParser.ParserError, ValidationError] with
    override def apply(x: IDParser.ParserError): ValidationError =
      ValidationError.ParserError(x)
end HardwareIDValidationService


class SimpleHardwareIDValidationService extends HardwareIDValidationService:

  import HardwareIDValidationService.*
  import HardwareIDValidationService.ValidationError.*
  import org.itsadigitaltrust.common.Operators.*
  import org.itsadigitaltrust.common.*

  type ![T] = Result.Continuation[ParsedResult, ValidationError] ?=> T

  private final val multiplier = 3



  override def validate(input: String): ValidationResult =
    Result:
      IDParser(input, ProgramMode.isInHardDriveMode) match

        case Error(value) =>
          error(ParserError(value))
        case Success(value) =>
          val userCheckDigit = value.checkDigit.getOrElse("0").toInt
          val numberSeq: IndexedSeq[Char] =
            value.number match
              case Some(value) => value.iterator.toIndexedSeq
              case None =>
                error(ValidationError.ParserError(IDParser.ParserError.MissingNumber))
          val oddTotal = calculateOddSum(numberSeq)
          val evenTotal = calculateEvenSum(numberSeq)
          val calculatedCheckDigit = (oddTotal + evenTotal * multiplier) % 10

          if userCheckDigit != calculatedCheckDigit then
            error(ValidationError.IncorrectCheckDigit(s"$calculatedCheckDigit", s"$userCheckDigit"))
          else
              Result.success(value)

  private def calculateOddSum[T <: Char](numberSeq: IndexedSeq[T]): Int =
    numberSeq.getEvenIndexItems.map(_.asDigit).sum

  private def calculateEvenSum[T <: Char](numberSeq: IndexedSeq[T]): Int =
    numberSeq.getOddIndexItems.map(_.asDigit).sum


  private def error(error: ValidationError): ![Nothing] =   Result.error(error)




//
//
//    def validate(input: String): Either[HardwareIDValidatonServiceError, Int] =
//        import HardwareIDValidatonServiceError.*
//        import SimpleHardwareIDValidatonService.*
//
//        def handleResult(result: ParsedResult): Either[HardwareIDValidatonServiceError, Int] =
//            if result.number.isEmpty then
//                return MissingNumber
//            else
//
//                val idTotal = Seq(number(0), number(2), number(4)).map(_.toInt).sum()
//                val checkTotal = Seq(number(1), number(3)).map(_.toInt).sum()
//                val total = idTotal + checkTotal
//                val checkDigit = total % 10
//                if result.checkDigit.isEmpty && checkDigit != 0 then
//                    return HardwareIDValidatonServiceError.IncorrectCheckDigit(checkDigit, 0)
//                else if result.checkDigit.nonEmpty && checkDigit != result.checkDigit.get.toInt then
//                    return HardwareIDValidatonServiceError.IncorrectCheckDigit(checkDigit, result.checkDigit.get.toInt)
//                else
//                    return checkDigit
//
//
//
//
//
//        if input.count(c => c == '.') > 1 then
//            return TooMenyDecimalPoints
//        else
//            idScanner(input) match
//                case Left(value) => return handleResult(value)
//                case Right(value) =>
//                    value match
//                        case ParsedResultError.InvalidCharacter(expected, got) => HardwareIDValidatonServiceError.InvalidCharacter(expected, s"$got")
//                        case ValidationError(error) => return error
//                        case ParsedResultError.End(string) => return TooShort
//
//            return 3
//
//object SimpleHardwareIDValidatonService:
//    final case class ParsedResult(prefix: Option[String] = None, number: Option[String] = None, decimal: Option[String] = None, checkDigit: Option[String] = None, suffix: Option[String] = None)
//
//    enum ParsedResultError:
//        case InvalidCharacter(expected: String, got: Char)
//        case ValidationError(error: HardwareIDValidatonServiceError)
//        case End(string: Option[String])
//
//    given Conversion[HardwareIDValidatonServiceError, ParsedResultError] with
//        def apply(x: HardwareIDValidatonServiceError): ParsedResultError =
//            ValidationError(x)
//
//    type ScannerResult = Either[ParsedResult, ParsedResultError]
//    given Conversion[ParsedResult, ScannerResult] with
//        def apply(x: ParsedResult): ScannerResult =
//            Left(x)
//
//    given Conversion[ParsedResultError, ScannerResult] with
//        def apply(x: ParsedResultError): ScannerResult =
//            Right(x)
//
//    given Conversion[HardwareIDValidatonServiceError, ScannerResult] with
//        def apply(x: HardwareIDValidatonServiceError): ScannerResult =
//            ValidationError(x)
//
//    val idPattern = "^([HKL])?(\\d{5})(\\.)(\\d)?$".r("prefix", "number", "decimalPoint", "checkDigit")
//
//
