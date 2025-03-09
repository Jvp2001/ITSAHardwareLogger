package org.itsadigitaltrust.hardwarelogger.mvvm.properties.accessorfunctions

import javafx.beans.property.Property

import java.util.function.Supplier

type SideEffect = () => Unit

private[mvvm] trait CommonBeanPropertyField[T, M, R <: Property[T], Target <: Property[T], Default](updateFunction: SideEffect,
                                                                                                    dt: T,
                                                                                                    val propertySupplier: Supplier[Target]):
  protected val targetProperty: Target = propertySupplier.get()
  protected var defaultValue: T = dt
  protected val getter: Getter[Target, M]

  val property: Target = targetProperty

  targetProperty.addListener: (observable, newValue, oldValue) =>
    updateFunction()


  def reload(wrappedObject: M): Unit =
    targetProperty.setValue(getter.apply(wrappedObject).getValue)

  def resetToDefault(): Unit =
    targetProperty.setValue(defaultValue)

  def updateDefault(wrappedObject: M): Unit =
    defaultValue = getter(wrappedObject).getValue

  def isDifferent(wrappedObject: M): Boolean =
    getter(wrappedObject).equals(targetProperty.getValue)


