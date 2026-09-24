package org.itsadigitaltrust.hardwarelogger.ui.derivation

import org.itsadigitaltrust.hardwarelogger.ui.derivation.annotations.UISchemaAnnotation
import scalafx.scene.Node

import scala.reflect.ClassTag

final case class DeferredField(
                                /**
                                 * Create the visual UI control
                                 */
                                controlBuilder: () => Node,
                                /**
                                 * Gets the value from the UI element created by [[DeferredField#controlBuilder]]. 
                                 */
                                flushValue: () => Any
                              )

final case class FieldUI(label: String, field: DeferredField, annotations: List[UISchemaAnnotation]):
  /** Checks if an annotation of a specific type exists on the field.
   * Example: field.hasAnnotation[Readonly]
   */
  def hasAnnotation[A <: UISchemaAnnotation: ClassTag as tag]: Boolean =
    annotations.exists(tag.runtimeClass.isInstance)

  /** Finds and extracts the first annotation instance matching a specific type.
   * Example: field.getAnnotation[DisplayName].map(_.name)
   */
  def getAnnotation[A <: UISchemaAnnotation: ClassTag as tag]: Option[A] =
    annotations.collectFirst:
      case a: A if tag.runtimeClass.isInstance(a) => a 
