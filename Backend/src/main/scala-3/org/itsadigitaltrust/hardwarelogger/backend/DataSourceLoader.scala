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

import java.sql.SQLException
import scala.util.Try



final class HLSqlDataSource extends MysqlDataSource
type DBProperties = Dict:



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

trait DataSourceLoaderService[DS] (using localhost: Boolean = false) extends HWLLoggable:
  lazy val dataSource: Either[HLDBErrorCode, DS]


end DataSourceLoaderService



final class DataSourceLoader(using localhost: Boolean = false) extends DataSourceLoaderService[HLSqlDataSource](using localhost):

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
            ?? ConnectionError( "Address").fail()
      end result
      result
  end ip
  override lazy val dataSource: Either[HLDBErrorCode,HLSqlDataSource] =
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
  end dataSource
end DataSourceLoader



object DataSourceLoader:
  given default: DataSourceLoader = DataSourceLoader()


object DataSourceLoaderService:
  type Error = PropertyFileReaderErrorCode
  
