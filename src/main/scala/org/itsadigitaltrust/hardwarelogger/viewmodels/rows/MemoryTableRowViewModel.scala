package org.itsadigitaltrust.hardwarelogger.viewmodels.rows

import javafx.beans.property.SimpleStringProperty
import scalafx.beans.property.*
import org.itsadigitaltrust.hardwarelogger.core.BeanConversions.given
import org.itsadigitaltrust.hardwarelogger.models.Memory
import org.itsadigitaltrust.hardwarelogger.mvvm.ModelWrapper
import org.itsadigitaltrust.hardwarelogger.viewmodels.TableRowViewModel

final class MemoryTableRowViewModel(model: Memory) extends TableRowViewModel[Memory](model):
  def sizeProperty: LongProperty =
    wrapper.field[Long, LongProperty, LongProperty]("size", _.size, 0)(LongProperty.apply)


  def descriptionProperty: StringProperty =
    wrapper.field("description", _.description, "")(StringProperty.apply)