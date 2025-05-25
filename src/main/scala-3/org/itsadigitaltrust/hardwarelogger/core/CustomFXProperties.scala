package org.itsadigitaltrust.hardwarelogger.core

import org.itsadigitaltrust.common.types.*
import scalafx.beans.property.ObjectProperty


final class DataSizeProperty extends ObjectProperty[DataSize]:
  override def toString: String =
    value.dbString

end DataSizeProperty

object DataSizeProperty:
  def apply(dataSize: DataSize): DataSizeProperty =
    val property = new DataSizeProperty
    property.value = dataSize
    property
    
end DataSizeProperty
