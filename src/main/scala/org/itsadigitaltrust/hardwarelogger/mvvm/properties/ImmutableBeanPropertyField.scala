package org.itsadigitaltrust.hardwarelogger.mvvm.properties

import javafx.beans.property.Property
import accessorfunctions.{CommonBeanPropertyField, Getter, ImmutableSetter, SideEffect}

import java.util.function.Supplier


final class ImmutableBeanPropertyField[T, M, R <: Property[T], PS <: Property[T], Default](
                                                                                            var updateFunction: SideEffect,
                                                                                            protected val getter: Getter[PS, M],
                                                                                            private val immutableSetter: ImmutableSetter[M, T],
                                                                                            defaultValue: T = null.asInstanceOf[T],
                                                                                            propertySupplier: Supplier[PS]
                                                                                          ) extends ImmutablePropertyField[T, M, PS] with CommonBeanPropertyField[T, M, R, PS, T](updateFunction, defaultValue, propertySupplier):

  override def commitImmutable(wrappedObject: M): M =
    immutableSetter(wrappedObject, targetProperty.getValue)