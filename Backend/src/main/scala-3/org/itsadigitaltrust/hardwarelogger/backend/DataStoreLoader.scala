package org.itsadigitaltrust.hardwarelogger.backend

import com.mysql.cj.jdbc.MysqlDataSource
import org.itsadigitaltrust.common
import org.itsadigitaltrust.common.optional.?
import org.itsadigitaltrust.common.{Result, Success, optional}

import java.io.{File, FileInputStream, FileNotFoundException, FileReader}
import java.net.URI
import java.util.Properties
import javax.sql.DataSource
import scala.util.{Failure, Using, boundary}

trait URLPropertyNameGetter:
  def get(properties: Properties): Option[String]

object DataStoreLoader:
  enum Error:
    case PropertyNotFound(name: String)
    case FileNotFound(name: String)

    override def toString: String = this match
      case Error.PropertyNotFound(name) => s"Cannot find property with $name!"
      case FileNotFound(name) => s"Cannot find file $name!"

  import scala.compiletime.*

  opaque type PropertyFileName = String

  object PropertyFileName:
    inline def apply(file: String): PropertyFileName =
      inline if file == "" || file == null then error(codeOf(file) + " cannot be empty or null!")
      //      else if file.split("/")(0) != "db.properties" then error (codeOf(file) + " must end with .properties!")
      else file


  private type Prefix = "MYSQL" | "ORACEL"


  def apply(configFile: URI, connectionPropName: String): Result[MysqlDataSource, Error] =
    Result:
      val dataSource = MysqlDataSource()
      val file = new File(configFile)

      val props = new Properties()
      val use = Using(new FileInputStream(new File(configFile))): fis =>
        props.load(fis)
      use match
        case Failure(exception) =>
          exception match
            case _: FileNotFoundException => Result.error(Error.FileNotFound(configFile.getRawPath))
        case util.Success(_) => ()
      
        Map[String, (MysqlDataSource, String) => Unit](
         connectionPropName -> ((ds: MysqlDataSource, v: String) =>
          ds.setURL(v)
          ),
        "db.username" -> ((ds, v) =>
          ds.setUser(v)
          ),
        "db.password" -> ((ds, v) =>
          ds.setPassword(v)
          )
      ).foreach: prop =>
        if !props.containsKey(prop._1) then
          Result.error(Error.PropertyNotFound(prop._1))
        else
          prop._2(dataSource, props.getProperty(prop._1))

      Result.success(dataSource)



