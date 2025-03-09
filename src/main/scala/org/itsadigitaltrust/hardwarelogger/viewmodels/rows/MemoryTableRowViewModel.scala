package org.itsadigitaltrust.hardwarelogger.viewmodels.rows

import javafx.beans.property.*
import org.itsadigitaltrust.hardwarelogger.core.conversions.given
import org.itsadigitaltrust.hardwarelogger.models.Memory
import org.itsadigitaltrust.hardwarelogger.mvvm.ModelWrapper
import org.itsadigitaltrust.hardwarelogger.viewmodels.TableRowViewModel

final class MemoryTableRowViewModel(model: Memory) extends TableRowViewModel[Memory](model):
  def sizeProperty: IntegerProperty =
    wrapper.field("size", _.size, (m, v) => m.size = v.toString.toInt, 0)(() =>  new SimpleIntegerProperty())

  def descriptionProperty: StringProperty =
    wrapper.field("size", _.description, (m, v) => m.description = v, "")(() =>  new SimpleStringProperty())


