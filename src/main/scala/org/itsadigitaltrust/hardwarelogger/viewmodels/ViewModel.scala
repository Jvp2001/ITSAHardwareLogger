package org.itsadigitaltrust.hardwarelogger.viewmodels

import org.itsadigitaltrust.hardwarelogger.mvvm.ModelWrapper
import org.springframework.stereotype.Component

@Component
trait ViewModel

@Component
trait TableRowViewModel[M](model: M) extends ViewModel:
  protected val wrapper: ModelWrapper[M] = ModelWrapper(model)