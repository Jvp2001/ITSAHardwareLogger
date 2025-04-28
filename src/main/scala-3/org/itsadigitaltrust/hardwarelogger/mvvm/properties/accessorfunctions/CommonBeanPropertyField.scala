package org.itsadigitaltrust.hardwarelogger.mvvm.properties.accessorfunctions

import scalafx.beans.property.Property

import java.util.function.Supplier

type SideEffect = () => Unit

private[mvvm] trait CommonBeanPropertyField[T, J, M, R <: Property[T, J], Target <: R](updateFunction: SideEffect,
                                                                                                          dt: J,
                                                                                                          val propertySupplier: T => R):
  val targetProperty: R = propertySupplier(null.asInstanceOf[T])
  protected var defaultValue: J = dt
  protected val getter: Getter[T, M]

  val property: R = targetProperty

  targetProperty.addListener: (observable, newValue, oldValue) =>
    updateFunction()


  def reload(wrappedObject: M): Unit =
    targetProperty.setValue(getter.apply(wrappedObject).asInstanceOf[J])

  def resetToDefault(): Unit =
    targetProperty.setValue(defaultValue)

  def updateDefault(wrappedObject: M): Unit =
    defaultValue = getter(wrappedObject).asInstanceOf[J]

  def isDifferent(wrappedObject: M): Boolean =
    getter(wrappedObject).equals(targetProperty.getValue)


