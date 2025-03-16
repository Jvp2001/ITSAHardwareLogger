package org.itsadigitaltrust.hardwarelogger.viewmodels

import org.itsadigitaltrust.hardwarelogger.mvvm.ModelWrapper


trait ViewModel


trait TableRowViewModel[M](model: M) extends ViewModel:
  protected val wrapper: ModelWrapper[M] = ModelWrapper(model)
  
  
