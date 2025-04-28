package org.itsadigitaltrust.hardwarelogger.mvvm.properties

import scalafx.beans.property.Property
import org.itsadigitaltrust.hardwarelogger.mvvm.ModelWrapper

import scala.sys.error
// Based off https://github.com/sialcasa/mvvmFX/blob/develop/mvvmfx/src/main/java/de/saxsys/mvvmfx/utils/mapping/PropertyField.java

/**
 * This interface defines the operations that are possible for each field of a wrapped class.
 *
 * @tparam T
 * target type. The base type of the returned property, f.e. [[String]].
 * @tparam M
 * model type. The type of the Model class, that is wrapped by this ModelWrapper instance.
 * @tparam R
 * return type.
 * The type of the Property that is returned via [[#getProperty()]], f.e.
 * [[scalafx.beans.property.StringProperty]] or [[Property[String]].
 */
trait PropertyField[T, J, M, R <: Property[T, J]]:

  def commit(wrappedObject: M): Unit

  def reload(wrappedObject: M): Unit

  def resetToDefault(): Unit

  def updateDefault(wrappedObject: M): Unit

  val targetProperty: R

  def getProperty: R = targetProperty

  /**
   * Determines if the value in the model object and the property field are different or not.
   *
   * This method is used to implement the [[ModelWrapper#differentProperty( )]] flag.
   *
   * @param wrappedObject
   * the wrapped model object
   * @return <code>false</code> if both the wrapped model object and the property field have the same value,
   *         otherwise <code>true</code>
   */
  def isDifferent(wrappedObject: M): Boolean


end PropertyField


trait ImmutablePropertyField[T, J, M, R <: Property[T, J]] extends PropertyField[T, J, M, R]:
  def commitImmutable(wrappedObject: M): M

  override final def commit(wrappedObject: M): Unit =
    error("commit is not supported on an immutable value!")

