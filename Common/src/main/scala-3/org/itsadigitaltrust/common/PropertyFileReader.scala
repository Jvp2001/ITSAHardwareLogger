package org.itsadigitaltrust.common

import java.io.{FileInputStream, FileNotFoundException}
import java.util.Properties
import scala.compiletime.ops.int.==
import scala.reflect.{ClassTag, classTag}
import scala.util.Using


opaque type PropertyFileName = String

/**
 * ^ - Matches the start of the string.
 * .*[\\/] - Matches any characters (including directories) up to the last / or \ (to handle both Unix and Windows paths).
 * [^\\/]+ - Matches the file name (excluding slashes).
 * \.properties - Matches the specific file extension .properties.
 * $ - Matches the end of the string.
 * 
 * This regex was defined and explained by Microsoft's Copilot.
 */
private inline val regex = "^.*[\\/][^\\/]+\\.properties$"

object PropertyFileName:

  import scala.compiletime.*
  import ops.string.*

  inline def apply[F <: String & Singleton](inline file: F): PropertyFileName =
    inline if !constValue[Matches[F, regex.type]] then
    error(s"${codeOf(file)}must end with .properties")
    //      else if file.split("/")(0) != "db.properties" then error (codeOf(file) + " must end with .properties!")
    else file

  def from(fileName: Option[String]): Option[PropertyFileName] =

    optional:
      if !fileName.?.endsWith(".properties") then s"${fileName.?}.properties" else fileName.?


end PropertyFileName

enum PropertyFileReaderError:
  case FileNotFound(fileName: String)
  case PropertyNotFound(propertyName: String)


class PropertyFileReader:

  import PropertyFileReader.!

  val props: Properties = new Properties()

  def read(file: PropertyFileName): ![PropertyFileReader] =
    Using(new FileInputStream(file.toString)): fis =>
      props.load(fis)
    match
      case scala.util.Success(_) => Result.success(this)
      case scala.util.Failure(exception) => exception match
        case _: FileNotFoundException => Result.error(PropertyFileReaderError.FileNotFound(file.toString))


  def get(key: String): ![String] =
    val value = Option(props.getProperty(key))
    value match
      case Some(value) => Result.success(value)
      case None => Result.error(PropertyFileReaderError.PropertyNotFound(key))

  private def error(e: PropertyFileReaderError): ![Nothing] =
    Result.error(e)
end PropertyFileReader


object PropertyFileReader:
  private type PrimitiveType = String | Float | Long | Double | Short | Boolean
  private type ![T] = Result.Continuation[T, PropertyFileReaderError] ?=> T

  import scala.compiletime.*
  import ops.string.*


  inline def apply(file: PropertyFileName): Result[PropertyFileReader, PropertyFileReaderError] =
    Result:
      val reader = new PropertyFileReader()
      reader.read(file)

  def from (propsFile: String): Result[PropertyFileReader, PropertyFileReaderError] =
    Result:

      val reader  = new PropertyFileReader()
      optional:
        val fileName = PropertyFileName.from(Option(propsFile))
        reader.read(fileName.?)
      .match
        case Some(r) => r
        case None => Result.error(PropertyFileReaderError.FileNotFound(propsFile))


  end from
  inline def apply[F <: String & Singleton](inline file: F): Result[PropertyFileReader, PropertyFileReaderError] =

    Result:
      inline if constValue[F `Matches` regex.type] then
        val reader = new PropertyFileReader()
        reader.read(PropertyFileName(file))
      else
        error(s"${codeOf(file)} must end with .properties!")
  end apply

  inline def apply[F <: String & Singleton](inline file: F)(inline key: String): Result[String, PropertyFileReaderError] =
    val reader = new PropertyFileReader()
    Result:
      Result:
        inline if constValue[F `Matches` regex.type] then
          reader.read(PropertyFileName(file))
        else
          error("file must end with .properties!")
      reader.get(key)
  end apply
end PropertyFileReader





