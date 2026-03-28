package org.itsadigitaltrust.hardwarelogger.services


import java.util.regex.Pattern
import scala.annotation.tailrec
import scala.util.boundary
import org.itsadigitaltrust.common.*

import org.itsadigitaltrust.hardwarelogger.delegates.ProgramMode
import org.itsadigitaltrust.hardwarelogger.services.IDParser.ParsedResult

import ox.either.fail


trait HardwareIDValidationService extends FrontendService:

  import HardwareIDValidationService.*

  def validate(input: String): ValidationResult
end HardwareIDValidationService

trait HardwareIDValidationServiceComponent:
  val hardwareIDValidationService: HardwareIDValidationService

object HardwareIDValidationService:
  type ValidationResult = Either[ValidationError, ParsedResult]
  extension (result: ValidationResult)
    def toBoolean: Boolean =
      result match
        case Right(_) => true
        case Left(_) => false


  enum ValidationError:
    case ParserError(error: IDParser.ParserError)
    case IncorrectCheckDigit(expected: String, got: String)

    override def toString: String =
      this match
        case ParserError(error) => error.toString()
        case IncorrectCheckDigit(expected, got) => s"Not a valid ID. Check if the ID is correct on the device."

  given Conversion[IDParser.ParserError, ValidationError] with
    override def apply(x: IDParser.ParserError): ValidationError =
      ValidationError.ParserError(x)
end HardwareIDValidationService


private[services] class SimpleHardwareIDValidationService extends HardwareIDValidationService:

  import HardwareIDValidationService.*
  import HardwareIDValidationService.ValidationError.*
  import org.itsadigitaltrust.common.Operators.*
  import org.itsadigitaltrust.common.*


  private final val multiplier = 3

  override def validate(input: String): ValidationResult =
    IDParser(input, ProgramMode.isInHardDriveMode) match
        case Right(value) =>
          val userCheckDigit = value.checkDigit.getOrElse("0").toInt
          validateCheckDigit(value, userCheckDigit)
        case Left(error) => Left(error)
  end validate

  private def validateCheckDigit(value: ParsedResult, userCheckDigit: Location) =
    ox.either:
      val numberSeq = value.number match
        case Some(value) => value.iterator.toIndexedSeq
        case None =>
          ValidationError.ParserError(IDParser.ParserError.MissingNumber).fail()
      val oddTotal = calculateOddSum(numberSeq)
      val evenTotal = calculateEvenSum(numberSeq)
      val calculatedCheckDigit = (oddTotal + evenTotal * multiplier) % 10
      if userCheckDigit != calculatedCheckDigit then
         ValidationError.IncorrectCheckDigit(s"$calculatedCheckDigit", s"$userCheckDigit").fail()
      else
        value
  end validateCheckDigit


  private def calculateOddSum[T <: Char](numberSeq: IndexedSeq[T]): Int =
    numberSeq.getEvenIndexItems.map(_.asDigit).sum

  private def calculateEvenSum[T <: Char](numberSeq: IndexedSeq[T]): Int =
    numberSeq.getOddIndexItems.map(_.asDigit).sum


end SimpleHardwareIDValidationService
