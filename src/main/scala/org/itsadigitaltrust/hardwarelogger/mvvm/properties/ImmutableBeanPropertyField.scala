package org.itsadigitaltrust.hardwarelogger.mvvm.properties

import scalafx.beans.property.Property
import accessorfunctions.{CommonBeanPropertyField, Getter, ImmutableSetter, SideEffect}

import java.util.function.Supplier


final class ImmutableBeanPropertyField[T, J, M, R <: Property[T, J], PS <: R]
(
  updateFunction: SideEffect,
  override val getter: Getter[T, M],
  val immutableSetter: ImmutableSetter[M, J],
  defaultValue: J = null.asInstanceOf[J],
  propertySupplier: T => PS
) extends ImmutablePropertyField[T, J, M, R] with CommonBeanPropertyField[T, J, M, R, PS](updateFunction, defaultValue, propertySupplier):
  override def commitImmutable(wrappedObject: M): M =
    immutableSetter(wrappedObject, targetProperty.getValue)