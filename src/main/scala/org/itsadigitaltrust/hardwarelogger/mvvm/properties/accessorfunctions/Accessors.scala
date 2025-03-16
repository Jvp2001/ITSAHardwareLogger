package org.itsadigitaltrust.hardwarelogger.mvvm.properties.accessorfunctions

import scalafx.beans.property.*

import java.lang


type PropertyAccessor[T, J, P <: Property[T, J], M] = M => P
type StringPropertyAccessor[M] = PropertyAccessor[String, String, StringProperty, M]
type NumberPropertyAccessor[T, P <: Property[T, Number], M] = PropertyAccessor[T, Number, P, M]
type FloatPropertyAccessor[M] = NumberPropertyAccessor[Float, FloatProperty, M]
type DoublePropertyAccessor[M] = NumberPropertyAccessor[Double, DoubleProperty, M]
type IntPropertyAccessor[M] = NumberPropertyAccessor[Int, IntegerProperty, M]
type LongPropertyAccessor[M] = NumberPropertyAccessor[Long, LongProperty, M]
type BooleanPropertyAccessor[M] = PropertyAccessor[Boolean, lang.Boolean, BooleanProperty, M]

