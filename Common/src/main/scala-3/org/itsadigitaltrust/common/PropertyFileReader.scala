package org.itsadigitaltrust.common



import java.io.{File, FileInputStream, FileNotFoundException, InputStream, StringReader}
import java.net.URI
import java.util.Properties
import scala.compiletime.ops.int.==
import scala.reflect.{ClassTag, classTag}
import scala.util.{Failure, Try, Using}


enum PropertyFileReaderError:
  case FileNotFound(fileName: String)
  case PropertyNotFound(propertyName: String)

  override def toString: String = this match
    case PropertyNotFound(name) => s"Cannot find property with $name!"
    case FileNotFound(name) => s"Cannot find file $name!"
end PropertyFileReaderError

class PropertyFileReader:

  import PropertyFileReader.!

  val props: Properties = new Properties()


  def apply(propName: String): Option[String] =
    Option(props.getProperty(propName))

  def apply(propName: String, default: String): String =
    apply(propName) ?? default

  def readString(string: Try[String]): Try[PropertyFileReader] =
    string.map: str =>
      props.load(new StringReader(str))
      this



  def read(file: Try[String]): ![PropertyFileReader] =
    file match
      case scala.util.Success(string: String) => readString(scala.util.Success(string))
      case scala.util.Failure(exception) => scala.util.Failure(exception)
    match
      case scala.util.Success(_) => Result.success(this)
      case scala.util.Failure(exception) => exception match
        case _: FileNotFoundException => Result.error(PropertyFileReaderError.FileNotFound(file.toString))


  private transparent inline def toPrimitiveType[T <: PrimitiveType : ClassTag](value: String): T =
    val result = inline summon[ClassTag[T]] match
      case classTag: ClassTag[String] => value
      case classTag: ClassTag[Float] => value.toFloat
      case classTag: ClassTag[Long] => value.toLong
      case classTag: ClassTag[Double] => value.toDouble
      case classTag: ClassTag[Short] => value.toShort
      case classTag: ClassTag[Boolean] => value.toBoolean
      case _ => value.asInstanceOf[T]
    result.asInstanceOf[T]
  end toPrimitiveType

  def update(key: String, value: PrimitiveType): Unit =
    props.setProperty(key, value.toString)

  def set(key: String, value: PrimitiveType): Unit =
    props.setProperty(key, value.toString)

  def setProperty(key: String, value: String): Unit =
    props.setProperty(key, value)


  import org.itsadigitaltrust.common.Default.given
  import PropertyFileReader.given
  def getProperty(key: String, default: String): String =
    getOrDefault(key, default)

  def getOrDefault(key: String, default: String): String =
    import org.itsadigitaltrust.common.Operators.??
    val value = Option(props.getProperty(key))
    value ?? default

  def containsKey(key: String): Boolean =
    props.containsKey(key)

  def remove(key: String): Unit =
    props.remove(key)

  private def error(e: PropertyFileReaderError): ![Nothing] =
    Result.error(e)



end PropertyFileReader


object PropertyFileReader:
  export Types.PrimitiveType


  private type ![T] = Result.Continuation[T, PropertyFileReaderError] ?=> T


  import scala.compiletime.*
  import ops.string.*


  inline def !(file: Try[String]): ![PropertyFileReader] =
    val reader = new PropertyFileReader()
    reader.read(file)


  inline def apply(file: Try[String]): Result[PropertyFileReader, PropertyFileReaderError] =
    Result:
      val reader = new PropertyFileReader()
      reader.read(file)

end PropertyFileReader





