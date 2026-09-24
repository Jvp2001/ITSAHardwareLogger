package org.itsadigitaltrust.common.configformats

trait ToConfigFormat[T]:
  def apply(instance: T, indentSpaces: Int = 0): String
end ToConfigFormat

object ToConfigFormat:

  inline def apply[T](instance: T)( using configFormatter: ConfigFormatter = defaultFormatter): String =
    derived[T].apply(instance)

  // Pure runtime factory helper block - binds the encoder and your custom formatter trait
  def manual[T](
                 encoder: T => CaseClassConfigInfo
               )(using formatter: ConfigFormatter): ToConfigFormat[T] =
    (instance: T, indentSpaces: Int) =>
      val configInfo = encoder(instance)
      formatter.format(configInfo, indentSpaces)
  end manual

  extension [T](instance: T)(using formatter: ToConfigFormat[T])
    def toConfigString(indentSpaces: Int = 0): String =
      formatter(instance, indentSpaces)

  def format[T](instance: T, indentSpaces: Int = 0)(using formatter: ToConfigFormat[T]): String =
    formatter(instance, indentSpaces)

  // Automatically pairs the macro layout function with whichever ConfigFormatter trait instance is in scope
  inline given derived[T](using formatter: ConfigFormatter): ToConfigFormat[T] =
    val encoderFunction = ConfigConversionMacros.mappingFunction[T]
    ToConfigFormat.manual[T](encoderFunction)

  // Provide the default Java properties engine implicitly to the scope boundary
  given defaultFormatter: ConfigFormatter = ConfigFormatter.JavaProperties

end ToConfigFormat
