// scala-3/org/itsadigitaltrust/hardwarelogger/backend/DataSourceLoader.scala
package org.itsadigitaltrust.hardwarelogger.backend

import com.mysql.cj.jdbc.MysqlDataSource
import org.itsadigitaltrust.common.Operators.??
import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.hardwarelogger.backend.HLDBErrorCode.ConnectionError
import org.itsadigitaltrust.hardwarelogger.backend.utils.IPAddressFinder
import ox.either.{fail, ok}
import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.generic.derivation.*

import java.sql.SQLException
import scala.Conversion.into
import scala.language.postfixOps
import scala.util.Try

final class HLSqlDataSource extends MysqlDataSource

final case class DBProperties(
                               name: String,
                               username: String,
                               password: String,
                               port: Int,
                               maxReconnects: Int,
                               autoReconnect: Boolean,
                               serverTimeZone: String,
                               unit7Address: String,
                               unit9Address: String,
                               fallbackAddress: String,
                               localhostUsername: String,
                               localhostPassword: String,
                               timeout: Int
                             ) derives ConfigReader

object DBProperties:
  given default: DBProperties = DBProperties(
    name = "hwlogger",
    maxReconnects = 5,
    serverTimeZone = "UTC",
    username = "itsa",
    password = "itsa123",
    unit7Address = "10.0.1.230",
    unit9Address = "192.168.100.230",
    fallbackAddress = "localhost",
    localhostUsername = "itsa",
    localhostPassword = "itsa123",
    timeout = 5,
    port = 3306,
    autoReconnect = true
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
    ox.either:
      IPAddressFinder.findDatabaseAddress(dbProps.unit7Address, dbProps.unit9Address) ?? {
        logger.warn("Could not match PC's IP to unit7's or unit9's IP address; falling back to the fallback address")
        dbProps.fallbackAddress
      }

  override lazy val dataSource: Either[HLDBErrorCode, HLSqlDataSource] =
    ip.map: addr =>
      given address: String = addr
      dbProps: HLSqlDataSource
object DataSourceLoader extends HWLLoggable:

  /**
   * Factory method to load configuration via PureConfig from a raw String or fall back gracefully.
   */
  def load(): Either[HLDBErrorCode, DataSourceLoader] =
    org.itsadigitaltrust.common.ConfigsManager.readConfig("db.properties")
      .toEither
      .left.map(err => HLDBErrorCode.PropertiesError)  // Explicitly widen to the base trait/enum
      .map: rawPropertiesString =>
        ConfigSource.string(rawPropertiesString).load[DBProperties] match
          case Right(props) => new DataSourceLoader(props)
          case Left(failures) =>
            logger.warn("Could not load DB properties from the file; falling back to the default.")
            new DataSourceLoader(DBProperties.default)

  def apply(): Either[HLDBErrorCode, DataSourceLoader] =
    load()
