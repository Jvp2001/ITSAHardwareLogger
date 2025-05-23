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
end HardwareIDValidationService

object HardwareIDValidationService:
  import Result.{Success, Error}
  type ValidationResult = Result[ParsedResult, ValidationError]
  extension (result: ValidationResult)
    def toBoolean: Boolean =
      result match
        case Success(_) => true
        case Error(_) => false


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
    import Result.{Success, Error}
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

  private def error(error: ValidationError): ![Nothing] = Result.error(error)

end SimpleHardwareIDValidationService
