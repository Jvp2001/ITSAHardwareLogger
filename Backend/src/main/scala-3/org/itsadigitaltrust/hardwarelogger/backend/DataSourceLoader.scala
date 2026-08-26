// scala-3/org/itsadigitaltrust/hardwarelogger/backend/DataSourceLoader.scala
package org.itsadigitaltrust.hardwarelogger.backend

import com.mysql.cj.jdbc.MysqlDataSource
import com.sun.tools.javac.util.Assert.error
import org.itsadigitaltrust.common
import org.itsadigitaltrust.common.Operators.|>
import org.itsadigitaltrust.common.collections.Dict
import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.common.propertyfile.{Key, PropertyFileDecoder, PropertyFileReader, PropertyFileReaderErrorCode}
import org.itsadigitaltrust.hardwarelogger.backend.HLDBErrorCode.ConnectionError
import org.itsadigitaltrust.hardwarelogger.backend.utils.IPAddressFinder
import ox.either.{fail, ok}

import java.sql.SQLException
import scala.language.postfixOps
import scala.util.Try


final class HLSqlDataSource extends MysqlDataSource

final case class DBProperties(

                               @Key("db.name") name: String,
                               @Key("db.username") username: String,
                               @Key("db.password") password: String,
                               @Key("db.port") port: Int,
                               @Key("db.maxReconnects") maxReconnects: Int,
                               @Key("db.autoReconnect") autoReconnect: Boolean,
                               @Key("db.serverTimeZone") serverTimeZone: String,
                               @Key("db.serverTimeZone") unit7Address: String,
                               @Key("db.unit9Address") unit9Address: String,
                               @Key("db.localAddress") localAddress: String,
                               @Key("db.useLocalhost") useLocalhost: Boolean,
                               @Key("db.localhostUsername") localhostUsername: String,
                               @Key("db.localhostPassword") localhostPassword: String,
                               @Key("db.timeout") timeout: Int,
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
    localAddress = "localhost",
    useLocalhost = true,
    localhostUsername = "itsa",
    localhostPassword = "itsa123",
    timeout = 5,
    port = 3306,
    autoReconnect = true,
  )

trait DataSourceLoaderService[DS](using localhost: Boolean = false) extends HWLLoggable:
  lazy val dataSource: Either[HLDBErrorCode, DS]


end DataSourceLoaderService


final class DataSourceLoader(defaultDbPros: Option[DBProperties] = None)(using localhost: Boolean) extends DataSourceLoaderService[HLSqlDataSource](using localhost), HWLLoggable:

  import org.itsadigitaltrust.common.Operators.??


  val unit7Address = "10.0.1.230"
  val unit9Address = "192.168.100.230"
  val ip: Either[HLDBErrorCode, String] =
    ox.either:
      val result =
        if localhost then
          "localhost"
        else
          IPAddressFinder.findDatabaseAddress(unit7Address, unit9Address)
            ?? ConnectionError("Address").fail()
      end result
      result
  end ip
  private def load(props: Option[DBProperties]): Either[HLDBErrorCode, HLSqlDataSource] =
    props.map: dbProps =>
      ip.map: address =>
        val ds = new HLSqlDataSource()
        ds.setDatabaseName(dbProps.name)
        ds.setAutoReconnect(dbProps.autoReconnect)
        ds.setLoginTimeout(dbProps.timeout)
        ds.setServerTimezone(dbProps.serverTimeZone)
        ds.setURL(s"jdbc:mysql://${address}:3306/${dbProps.name}?useSSL=false&serverTimezone=${dbProps.serverTimeZone}")
        ds.setUser(dbProps.username)
        ds.setPassword(dbProps.password)
        ds

    .getOrElse:
      ip.map: address =>
        val ds = new HLSqlDataSource()
        ds.setDatabaseName("hwlogger")
        ds.setAutoReconnect(true)
        ds.setLoginTimeout(5)
        ds.setServerTimezone("UTC")
        ds.setURL(s"jdbc:mysql://$address:3306/hwlogger?useSSL=false&serverTimezone=UTC")
        ds.setUser("itsa")
        ds.setPassword("itsa123")
        ds
  end load


  override lazy val dataSource: Either[HLDBErrorCode, HLSqlDataSource] =
    load(defaultDbPros)
  end dataSource



end DataSourceLoader


object DataSourceLoader:
  def apply(dbName: String)(using localhost:Boolean): DataSourceLoader =
    new DataSourceLoader(Option(DBProperties.default.copy(name = dbName)))

object DataSourceLoaderService:
  type Error = PropertyFileReaderErrorCode
  
