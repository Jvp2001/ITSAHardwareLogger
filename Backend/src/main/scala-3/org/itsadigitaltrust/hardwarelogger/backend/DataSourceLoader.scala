// scala-3/org/itsadigitaltrust/hardwarelogger/backend/DataSourceLoader.scala
package org.itsadigitaltrust.hardwarelogger.backend

import com.mysql.cj.jdbc.MysqlDataSource
import org.itsadigitaltrust.common
import org.itsadigitaltrust.common.collections.Dict
import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.common.{PropertyFileReader, PropertyFileReaderErrorCode, Result}
import org.itsadigitaltrust.hardwarelogger.backend.HLDBErrorCode.ConnectionError
import org.itsadigitaltrust.hardwarelogger.backend.utils.IPAddressFinder
import ox.either.{fail, ok}

import scala.util.Try


final class HLSqlDataSource extends MysqlDataSource

trait DataSourceLoaderService(propertiesFilePath: String) extends HWLLoggable:

  type Error = PropertyFileReader


  private type DBProperties = Dict:
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
      val useLocalhost: Boolean
      val localhostUsername: String
      val localhostPassword: String
      val timeout: Int


  extension (props: DBProperties)
    def isValid: Boolean =
      !(props.name.isBlank && props.username.isBlank && props.password.isBlank && props.unit7Address.isBlank && props.unit9Address.isBlank && props.localAddress.isBlank)

  private type DBAddresses = Dict:
      val unit7: String
      val unit9: String
      val local: String


  private var dbProperties: Option[DBProperties] = None
  private var _dataSource: Option[MysqlDataSource] = None

  private def dataSource_=(value: Option[MysqlDataSource]): Unit =
    _dataSource = value

  def dataSource: Option[MysqlDataSource] = _dataSource

  def apply(configFile: String): Either[PropertyFileReaderErrorCode, MysqlDataSource] =
    reload(configFile)

  def reload(configFile:String): Either[PropertyFileReaderErrorCode, MysqlDataSource] =

    this. _dataSource = _dataSource.orElse(Some(new HLSqlDataSource()))

    def createProperties(props: PropertyFileReader) =
      val dict: Dict = Dict:
        val name = props("db.name", "hwlogger")
        val username = props("db.username", "")
        val port = props("db.port", "3306").toInt
        val password = props("db.password", "")
        val maxReconnects = props("db.maxReconnects", "2").toInt
        val autoReconnect = props("db.autoReconnect", "true").toBoolean
        val serverTimeZone = props("db.serveTimeZone", "UTC")
        val unit7Address = props("db.unit7.address", "")
        val unit9Address = props("db.unit9.address", "")
        val localAddress = props("db.local.address", "")
        val useLocalhost = props("db.useLocalhost", "false").toBoolean
        val localhostUsername = props("db.localhost.username", "root")
        val localhostPassword = props("db.localhost.password", "root")
        val timeout = props("db.timeout", "1").toInt
      end dict
      dict

    ox.either:
      val propsFileReader = PropertyFileReader(configFile)
      val props: PropertyFileReader = propsFileReader.ok()
      if !(dbProperties.isDefined && dbProperties.get.isValid) then
        val dict: Dict = createProperties(props)
        dbProperties = Option(dict.asInstanceOf[DBProperties])
      end if

      val properties = dbProperties.get
      val url =
        if properties.useLocalhost then
          logger.info("Using localhost address")
          s"jdbc:mysql://localhost:${properties.port}/${properties.name}"
        else
          IPAddressFinder.findDatabaseAddress(properties.unit7Address, properties.unit9Address, properties.localAddress) match
            case Some(address) =>
              logger.info(s"Best Address: $address")
              s"jdbc:mysql://$address:${properties.port}/${properties.name}"
            case None =>
              logger.warn("Using local address")
              s"jdbc:mysql://${properties.localAddress}:${properties.port}/${properties.name}"
      end url
      dataSource.map: ds =>
        val username = if properties.useLocalhost then properties.localhostUsername else properties.username
        val password = if properties.useLocalhost then properties.localhostPassword else properties.password
        ds.setDatabaseName(properties.name)
        ds.setUrl(url)
        ds.setUser(username)
        ds.setPassword(password)
        ds.setMaxReconnects(properties.maxReconnects)
        ds.setServerTimezone(properties.serverTimeZone)
        ds.setAutoReconnect(properties.autoReconnect)
        ds.setConnectTimeout(properties.timeout)
        ds.setLoginTimeout(properties.timeout)
        ds.setUseSSL(false)
        ds.setAllowPublicKeyRetrieval(true)
        ds
      .get
  end reload

end DataSourceLoaderService


trait DataSourceLoaderFactory:
  type Loader <: DataSourceLoaderService
  def apply(properties: String): Loader
  
  

private[backend] final class DataSourceLoader(properties: String) extends DataSourceLoaderService(properties)
object DataSourceLoader:
  private[backend] given dataSourceLoaderFactory: DataSourceLoaderFactory with
    override type Loader = DataSourceLoader

    override def apply(properties: String): Loader = DataSourceLoader(properties)


object DataSourceLoaderService:
  type Error = PropertyFileReaderErrorCode
  
  def apply(properties: String)(using dataSourceLoaderFactory: DataSourceLoaderFactory = DataSourceLoader.dataSourceLoaderFactory): Either[Error, dataSourceLoaderFactory.Loader] =
    val dsl: dataSourceLoaderFactory.Loader = dataSourceLoaderFactory(properties)
    Right(dsl)