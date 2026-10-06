// scala-3/org/itsadigitaltrust/hardwarelogger/backend/DataSourceLoader.scala
package org.itsadigitaltrust.hardwarelogger.backend

import com.mysql.cj.jdbc.MysqlDataSource
import org.itsadigitaltrust.common.AssetLoader.asset
import org.itsadigitaltrust.common.Operators.{??, |>}
import org.itsadigitaltrust.common.{ConfigsManager, config}
import org.itsadigitaltrust.common.configformats.{ConfigComment, ConfigGroupPrefix, JavaPropertiesReader, PropertyReader, ToConfigFormat}
import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.hardwarelogger.backend.HLDBErrorCode.ConnectionError
import org.itsadigitaltrust.hardwarelogger.backend.utils.IPAddressFinder
import ox.either.{fail, ok}

import java.sql.SQLException
import scala.Conversion.into
import scala.language.postfixOps
import scala.util.Try

final class HLSqlDataSource extends MysqlDataSource

@ConfigGroupPrefix("db")
final case class DBProperties(
                             @ConfigComment(" The database name")
                               name: String,
                              @ConfigComment(" The database username")
                               username: String,
                             @ConfigComment(" The user's password")
                               password: String,

                             @ConfigComment(" The max number of reconnects")
                               maxReconnects: Int,
                             @ConfigComment(" Should the database auto reconnect")
                               autoReconnect: Boolean,
                             @ConfigComment(" The Timezone for the database")
                               serverTimeZone: String,
                             @ConfigComment(" The primary IP address to use to connect")
                               primaryAddress: String,
                             @ConfigComment(" The port number for the database")
                             port: Int,
                             @ConfigComment(" The fallback address for the database, only used if the primary address is unreachable.")
                               fallbackAddress: String,
                             @ConfigComment(" Timeout in milliseconds")
                               timeout: Int
                             ) 
object DBProperties:
  given default: DBProperties = DBProperties(
    name = "hwlogger",
    maxReconnects = 5,
    serverTimeZone = "UTC",
    username = "itsa",
    password = "itsa123",
    primaryAddress = "10.0.1.222",
    fallbackAddress = "localhost",
    timeout = 5,
    autoReconnect = true,
    port = 3306,
  )

  given toDataSource(using address: String): Conversion[DBProperties, HLSqlDataSource] with
    override def apply(dbProps: DBProperties): HLSqlDataSource =
      val ds = new HLSqlDataSource()
      ds.setDatabaseName(dbProps.name)
      ds.setPort(dbProps.port)
      ds.setAutoReconnect(dbProps.autoReconnect)
      ds.setLoginTimeout(dbProps.timeout)
      ds.setServerTimezone(dbProps.serverTimeZone)
      ds.setURL(s"jdbc:mysql://$address:${dbProps.port}/${dbProps.name}?useSSL=false&serverTimezone=${dbProps.serverTimeZone}")
      ds.setUser(dbProps.username)
      ds.setPassword(dbProps.password)
      ds

trait DataSourceLoaderService[DS <: MysqlDataSource](using localhost: Boolean = false) extends HWLLoggable:
  lazy val dataSource: Either[HLDBErrorCode, DS]

final class DataSourceLoader(val dbProps: DBProperties) extends DataSourceLoaderService[HLSqlDataSource], HWLLoggable:

  val ip: Either[HLDBErrorCode, String] =
    val result = ox.either:
        dbProps.primaryAddress
    println(s"IP: $result")
    result

  override lazy val dataSource: Either[HLDBErrorCode, HLSqlDataSource] =
    ip.map: addr =>
      given address: String = addr
      dbProps: HLSqlDataSource
object DataSourceLoader extends HWLLoggable:

  /**
   * Factory method to load configuration via PureConfig from a raw String or fall back gracefully.
   */
  def load(): Either[HLDBErrorCode, DataSourceLoader] =
//    org.itsadigitaltrust.common.ConfigsManager.readConfig(config"db.properties")
//      .toEither
//      .left.map(err => HLDBErrorCode.PropertiesError)  // Explicitly widen to the base trait/enum
//      .map: rawPropertiesString =>
    val encoded = ToConfigFormat(DBProperties.default)
    
    println("Reading config file")
    val readData = encoded |> JavaPropertiesReader.read[DBProperties]
    readData
    match
        case Right(props) =>
          logger.info(s"Loaded DB properties: $props")
          Right(new DataSourceLoader(props))
        case Left(failures) =>
          logger.warn("Could not load DB properties from the file; falling back to the default.")
          Right(new DataSourceLoader(DBProperties.default))

  def apply(): Either[HLDBErrorCode, DataSourceLoader] =
    load()


end DataSourceLoader

object ToConfigFormatTesting extends HWLLoggable:
  @main
  def testConfigFormat: Unit =
    val encoded = ToConfigFormat(DBProperties.default)

    println("Reading config file")
    val readData = encoded |> JavaPropertiesReader.read[DBProperties]
    println(readData)

