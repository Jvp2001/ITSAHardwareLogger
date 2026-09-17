package org.itsadigitaltrust.hardwarelogger.state

import org.itsadigitaltrust.common.Operators.??
import org.itsadigitaltrust.hardwarelogger.models.HLModel
import org.itsadigitaltrust.hardwarelogger.services.HardwareGrabberService
import org.itsadigitaltrust.hardwarelogger.services.database.HLDatabaseService
import org.itsadigitaltrust.hardwarelogger.shared.HardwareLoggerSharedData
import org.itsadigitaltrust.hardwarelogger.state.Macros.performBackendValidationOnDataImpl
import ox.Ox

import scala.language.postfixOps

private[state] object Macros:
  import quoted.*
  inline def performBackendValidationOnData(
                                           inline data: HardwareLoggerSharedData
                                         )(using inline databaseService: HLDatabaseService,
                                           hardwareGrabberService: HardwareGrabberService): Unit =
    ${ performBackendValidationOnDataImpl('data, 'databaseService) }

  private def performBackendValidationOnDataImpl(
                                                  data: Expr[HardwareLoggerSharedData],
                                                  databaseService: Expr[HLDatabaseService]
                                                )(using Quotes): Expr[Unit] =
    import quotes.reflect.*
  
    val tpe = TypeRepr.of[HardwareLoggerSharedData]
    val allFields = tpe.typeSymbol.caseFields
  
    val filteredFields = allFields.filter: field =>
      val fieldType = tpe.memberType(field)
      fieldType =:= TypeRepr.of[Option[Seq[? <: HLModel]]]
  
    val runValidation = Select.unique(databaseService.asTerm, "runValidation")
  
    if runValidation.symbol.isNoSymbol then
      report.errorAndAbort(
        s"Method 'performBackendValidationOnData' not found on ${databaseService.asTerm.show}"
      )
  
    val terms: List[Term] = filteredFields.map: field =>
      val fieldValue = Select.unique(data.asTerm, field.name) // fix: use field.name
      runValidation.appliedTo(fieldValue)
  
    if terms.isEmpty then
      '{ () }
    else
      Block(terms.dropRight(1), terms.last).asExprOf[Unit] // fix: proper Block   
  
  end performBackendValidationOnDataImpl
  
end Macros
