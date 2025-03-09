package org.itsadigitaltrust.hardwarelogger.mvvm.properties

import javafx.beans.property.Property
import org.itsadigitaltrust.hardwarelogger.mvvm.properties.accessorfunctions.*

import java.util.function.Supplier

final class BeanPropertyField[T, M, R <: Property[T], PS <: Property[T]]
(
  updateFunction: SideEffect,
  override val getter: Getter[PS, M],
  val setter: MutableSetter[M, T],
  defaultValue: T = null.asInstanceOf[T],
  propertySupplier: Supplier[PS]
) extends PropertyField[T, M, PS] with CommonBeanPropertyField[T, M, R, PS, R](updateFunction, defaultValue, propertySupplier):
  override def commit(wrappedObject: M): Unit =
    setter(wrappedObject, targetProperty.getValue)





