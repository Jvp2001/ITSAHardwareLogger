package org.itsadigitaltrust.hardwarelogger.mvvm.properties

import scalafx.beans.property.Property
import org.itsadigitaltrust.hardwarelogger.mvvm.properties.accessorfunctions.*

import java.util.function.Supplier

final class BeanPropertyField[T, J, M, R <: Property[T, J], PS <: R]
(
  updateFunction: SideEffect,
  val getter: Getter[T, M],
  val setter: MutableSetter[M, J],
  defaultValue: J = null.asInstanceOf[J],
  propertySupplier: T => PS
) extends PropertyField[T, J, M, R] with CommonBeanPropertyField[T, J, M, R, PS](updateFunction, defaultValue, propertySupplier):
  override def commit(wrappedObject: M): Unit =
    setter(wrappedObject, targetProperty.getValue)





