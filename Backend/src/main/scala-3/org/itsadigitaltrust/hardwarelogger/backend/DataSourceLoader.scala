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


final class HLSqlDataSource extends MysqlDataSource
  
object DataSourceLoader:
  enum Error:
    case PropertyNotFound(name: String)
    case FileNotFound(name: String)

    override def toString: String = this match
      case Error.PropertyNotFound(name) => s"Cannot find property with $name!"
      case FileNotFound(name) => s"Cannot find file $name!"

  import scala.compiletime.*

  private type Prefix = "MYSQL" | "ORACEL"

  private final val timeoutTime = 3 // seconds


  def apply(configFile: URI): Result[MysqlDataSource, Error] =
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
         "db.url" -> ((ds: MysqlDataSource, v: String) =>
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

      Seq(dataSource.setLoginTimeout, dataSource.setConnectTimeout, dataSource.setSocketTimeout).foreach(_(timeoutTime))
      Result.success(dataSource)



