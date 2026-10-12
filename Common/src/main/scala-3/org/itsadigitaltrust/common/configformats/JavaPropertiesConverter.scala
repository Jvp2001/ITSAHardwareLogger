package org.itsadigitaltrust.common.configformats

import scala.deriving.Mirror

object JavaPropertiesConverter:
  inline def apply[A](instance: A)(using M: Mirror.ProductOf[A]): java.util.Properties =
    val result = ConfigConversionMacros.mappingFunction(instance)
    val props = new java.util.Properties()
    result.fields.map: field =>
      val name =
        if result.groupPrefix.nonEmpty then
          result.groupPrefix + "." + field.name
        else
          field.name
      end name
      props.setProperty(name, field.value)
    props
  end apply

  inline given[A](using Mirror.ProductOf[A]): Conversion[A, java.util.Properties] = JavaPropertiesConverter.apply[A]
end JavaPropertiesConverter