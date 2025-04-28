package org.itsadigitaltrust.hardwarelogger.core

import scalafx.beans.binding.{Bindings, ObjectBinding}
import scalafx.beans.property.*
import scalafx.beans.value.ObservableValue

object BeanConversions:

  given[T, P] => Conversion[P, ObservableValue[T, T]] = _.asInstanceOf[ObservableValue[T,T]]
//  given[T, P] => Conversion[P, ObjectBinding[T]] = _.asInstanceOf[ObjectBinding[T]]

