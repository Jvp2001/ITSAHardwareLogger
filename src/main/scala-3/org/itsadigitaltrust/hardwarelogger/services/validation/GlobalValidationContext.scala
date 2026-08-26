package org.itsadigitaltrust.hardwarelogger.services.validation


import org.itsadigitaltrust.hardwarelogger.models.HLModel

import scala.collection.mutable
trait HLDBValidationError
trait CommonGlobalValidationContext[VI <: HLDBValidationError]:
  protected val issuesBuffer: mutable.ListBuffer[VI] = mutable.ListBuffer()
  
  def addIssue(issue: VI): Unit = synchronized:
    issuesBuffer += issue
    
  def getIssues: List[VI] = synchronized:
    issuesBuffer.toList
    
  def hasErrors: Boolean = synchronized:
    issuesBuffer.nonEmpty
    
  def += (issue: VI): Unit = addIssue(issue)
  
  def clear(): Unit = issuesBuffer.clear()
  
    
end CommonGlobalValidationContext


final case class ValidationIssue(
                                source: String,
                                message: String,
                                requiresModeSwitch: Boolean = false,
                                isSoftWarning: Boolean = false
                                ) extends HLDBValidationError
final class GlobalValidationContext[+M <: HLModel] extends CommonGlobalValidationContext[ValidationIssue]
  
object GlobalValidationContext
