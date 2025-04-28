package org.itsadigitaltrust.hardwarelogger.mvvm.properties

import org.itsadigitaltrust.hardwarelogger.mvvm.properties.accessorfunctions.{PropertyAccessor, SideEffect}
import scalafx.beans.property.*
import scalafx.Includes.*


class FxPropertyField[T, J, M, R <: Property[T, J], PS <: R](
                                                  private var defaultValue: T,
                                                  private val accessor: PropertyAccessor[T, J, R, M],
                                                  override val targetProperty: R
                                                ) extends PropertyField[T, J, M, R]:

  def this(updateFunction: SideEffect, accessor: PropertyAccessor[T, J, R, M], defaultValue: T = null.asInstanceOf[T] , propertySupplier: T => PS) =
    this(defaultValue, accessor, propertySupplier(defaultValue))
    this.targetProperty.addListener: (observable, oldValue, newValue) =>
      updateFunction()

  override def commit(wrappedObject: M): Unit =
    accessor(wrappedObject).value = targetProperty.value

  override def reload(wrappedObject: M): Unit =
    targetProperty.value = accessor(wrappedObject).value

  override def resetToDefault(): Unit =
    targetProperty.value =  defaultValue

  override def isDifferent(wrappedObject: M): Boolean =
    val modelValue: T = accessor(wrappedObject).value
    val wrapperValue = targetProperty.value

    modelValue != wrapperValue

  override def updateDefault(wrappedObject: M): Unit =
    defaultValue = accessor(wrappedObject).value



