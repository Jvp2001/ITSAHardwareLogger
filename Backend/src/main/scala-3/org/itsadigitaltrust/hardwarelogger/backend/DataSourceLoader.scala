// scala-3/org/itsadigitaltrust/hardwarelogger/backend/DataSourceLoader.scala
package org.itsadigitaltrust.hardwarelogger.backend

import com.mysql.cj.jdbc.MysqlDataSource
import org.itsadigitaltrust.common.Operators.??
import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.hardwarelogger.backend.HLDBErrorCode.ConnectionError
import org.itsadigitaltrust.hardwarelogger.backend.utils.IPAddressFinder
import ox.either.{fail, ok}

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
                               timeout: Int,
                             )

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
    autoReconnect = true,
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
  end toDataSource
  
end DBProperties

  

trait DataSourceLoaderService[DS](using localhost: Boolean = false) extends HWLLoggable:
  lazy val dataSource: Either[HLDBErrorCode, DS]


end DataSourceLoaderService


final class DataSourceLoader(dbProps: Option[DBProperties] = None) extends DataSourceLoaderService[HLSqlDataSource], HWLLoggable:


  /**
   * Gets the value for a property, if the given [[DataSourceLoader#dbProps]] is none, then it will use [[DBProperties#default]] instead.
   * @param get a function that takes in a [[DBProperties]] and return the value of a given property of the [[DBProperties]]'s instance.
   * @tparam R the type of the property that is being return by the get parameter function
   * @return the type of the property.
   */
  private def apply[R](get: DBProperties => R): R =
    dbProps.map(get) ?? get(DBProperties.default)

  private val unit7Address = this(_.unit7Address)
  private val unit9Address = this(_.unit9Address)
  private val fallbackAddress = this(_.fallbackAddress)
  val ip: Either[HLDBErrorCode, String] =
    ox.either:
      val result =
        IPAddressFinder.findDatabaseAddress(unit7Address, unit9Address)
          ?? {
          logger.warn("Could not match PC's IP to unit7's or unit9's IP address; falling back to the fallback address")
          fallbackAddress
        }
      end result
      result
  end ip
  private def load(props: Option[DBProperties]): Either[HLDBErrorCode, HLSqlDataSource] =
    ip.map: addr =>
      given address: String = addr
      props ?? DBProperties.default
  end load


  override lazy val dataSource: Either[HLDBErrorCode, HLSqlDataSource] =
    load(dbProps)
  end dataSource

end DataSourceLoader


object DataSourceLoader:
  def apply(dbName: String)(using localhost:Boolean): DataSourceLoader =
    new DataSourceLoader(Option(DBProperties.default.copy(name = dbName)))

object DataSourceLoaderService
  
