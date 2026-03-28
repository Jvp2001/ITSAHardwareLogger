package org.itsadigitaltrust.common

import java.io.{File, FileInputStream, FileNotFoundException, InputStream, StringReader}
import java.net.URI
import java.util.Properties
import scala.compiletime.ops.int.==
import scala.reflect.{ClassTag, classTag}
import scala.util.{Failure, Success, Try, Using, boundary}
import Types.Result
import org.itsadigitaltrust.common.Operators.??

import ox.either.{fail, ok}

enum PropertyFileReaderErrorCode:
  case FileNotFound(fileName: String)
  case PropertyNotFound(propertyName: String)

  override def toString: String = this match
    case PropertyNotFound(name) => s"Cannot find property with $name!"
    case FileNotFound(name) => s"Cannot find file $name!"
end PropertyFileReaderErrorCode

class PropertyFileReader:

  import PropertyFileReader.!

  val props: Properties = new Properties()


  def apply(propName: String): Option[String] =
    Option(props.getProperty(propName))

  def apply(propName: String, default: String): String =
    apply(propName) ?? default

  def readString(string: Option[String]): Result[PropertyFileReader, PropertyFileReaderErrorCode] = Result:
    string.foreach: str =>
      props.load(new StringReader(str))
    this



  def read(file: String): Either[PropertyFileReaderErrorCode, PropertyFileReader] =
    ox.either:
      val fileName = file ?? ""
      if fileName.isEmpty || !File(fileName).exists() then
        PropertyFileReaderErrorCode.FileNotFound(fileName).fail()
      this
  end read
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
    import org.itsadigitaltrust.common.Operators.or
    val value = Option(props.getProperty(key))
    value ?? default

  def containsKey(key: String): Boolean =
    props.containsKey(key)

  def remove(key: String): Unit =
    props.remove(key)



end PropertyFileReader


object PropertyFileReader:
  export Types.PrimitiveType

  import scala.util.boundary.Label


  private type ![T] = Either[T, PropertyFileReaderErrorCode] => T


  import scala.compiletime.*
  import ops.string.*




  inline def apply(file: String): Either[PropertyFileReaderErrorCode, PropertyFileReader] =
    ox.either:
      val reader = new PropertyFileReader()
      reader.read(file).ok()



end PropertyFileReader





