package org.itsadigitaltrust.hardwarelogger.backend

import com.mysql.cj.jdbc.MysqlDataSource
import org.itsadigitaltrust.common
import org.itsadigitaltrust.common.Operators.{??, |>}
import org.itsadigitaltrust.common.collections.Dict

import org.itsadigitaltrust.common.{PropertyFileReader, PropertyFileReaderError, Result, Success}
import org.itsadigitaltrust.hardwarelogger.backend.utils.IPAddressFinder

import java.io.{File, FileInputStream, FileNotFoundException, FileReader}
import java.net.{MalformedURLException, URI}
import java.nio.file.Path
import java.util.Properties
import javax.sql.DataSource
import scala.collection.immutable.{AbstractSeq, LinearSeq}
import scala.util.{Failure, Try, Using, boundary}


final class HLSqlDataSource extends MysqlDataSource

class DataSourceLoader private:
  type Error = PropertyFileReaderError

  import scala.compiletime.*

  type ![T] = Result.Continuation[T, PropertyFileReaderError] ?=> T

  private type DBProperties = Dict
    {
      val name: String
      val username: String
      val password: String
      val port: Int
      val maxReconnects: Int
      val autoReconnect: Boolean
      val serverTimeZone: String
      val unit7Address: String
      val unit9Address: String
      val localAddress: String
    }
  extension (props: DBProperties)
    def isValid: Boolean =
      !(props.name.isBlank && props.username.isBlank && props.password.isBlank && props.unit7Address.isBlank && props.unit9Address.isBlank && props.localAddress.isBlank)

  private type DBAddresses = Dict
    {
      val unit7: String
      val unit9: String
      val local: String
    }

  private var dbProperties: Option[DBProperties] = None
  private var _dataSource: Option[MysqlDataSource] = None

  private def dataSource_=(value: Option[MysqlDataSource]): Unit =
    _dataSource = value

  def dataSource: Option[MysqlDataSource] = _dataSource

  def apply(configFile: Try[URI]): Option[PropertyFileReaderError] = reload(configFile)

  def reload(configFile: Try[URI]): Option[PropertyFileReaderError] =
    val result: Result[MysqlDataSource, PropertyFileReaderError] =
      Result:
        dataSource = dataSource.orElse(Option(new MysqlDataSource()))

        if !(dbProperties.isDefined && dbProperties.get.isValid) then
          val propsFileReader: PropertyFileReader = PropertyFileReader(configFile) match
            case Success(value) => value
            case Result.Error(reason) => Result.error(reason)

          val props = propsFileReader



          val dict = Dict:
            val name = props("db.name", "hwlogger")
            val username = props("db.username", "")
            val port = props("db.port", "3306").toInt
            val password = props("db.password", "")
            val maxReconnects = props("db.maxReconnects", "10").toInt
            val autoReconnect = props("db.autoReconnect", "true").toBoolean
            val serverTimeZone = props("db.serveTimeZone", "UTC")
            val unit7Address = props("db.unit7.address", "")
            val unit9Address = props("db.unit9.address", "")
            val localAddress = props("db.local.address", "")
          end dict
          dbProperties = Option(dict.asInstanceOf[DBProperties])
        end if

        val props = dbProperties.get
        val url = IPAddressFinder.findDatabaseAddress(props.unit7Address, props.unit9Address, props.localAddress) match
          case Some(address) => s"jdbc:mysql://$address:${props.port}/${props.name}"
          case None => s"jdbc:mysql://${props.localAddress}:${props.port}/${props.name}"

        val ds = dataSource.get
        ds.setDatabaseName(props.name)
        ds.setUrl(url)
        ds.setUser(props.username)
        ds.setPassword(props.password)
        ds.setMaxReconnects(props.maxReconnects)
        ds.setServerTimezone(props.serverTimeZone)
        ds.setAutoReconnect(props.autoReconnect)
        Result.success(ds)
    end result

    result.toOptionError
  end reload

end DataSourceLoader

object DataSourceLoader:
  type Error = PropertyFileReaderError

  def apply(configFile: Try[URI]): Result[DataSourceLoader, PropertyFileReaderError] =
    val dsl = new DataSourceLoader
    Result:
      dsl.reload(configFile)
      Result.success(dsl)

end DataSourceLoader
