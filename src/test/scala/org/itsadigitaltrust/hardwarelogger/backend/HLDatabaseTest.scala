package org.itsadigitaltrust.hardwarelogger.backend

import org.itsadigitaltrust.common.Operators.|>

import org.itsadigitaltrust.hardwarelogger.backend.HLDatabase.Error
import org.itsadigitaltrust.hardwarelogger.models.HLModel
import org.itsadigitaltrust.hardwarelogger.services.{CommonHLDatabase, HLDatabaseService, HLDatabaseServiceException, HardwareGrabberService}
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{DBSuccessArgs, NotificationCentreService, NotificationName}
import org.itsadigitaltrust.hardwarelogger.tasks.HardwareLoggerTaskContext

import com.mysql.cj.jdbc.MysqlDataSource
import ox.either.fail

import scala.reflect.ClassTag
private given dataSourceLoaderFactory: DataSourceLoaderFactory = DataSourceLoaderFactoryTests()

final class DataSourceLoaderServiceTest(propertiesFile: String) extends DataSourceLoaderService(propertiesFile):
  override def dataSource: Option[MysqlDataSource] =
    val source = MysqlDataSource()
      source.setUrl("jdbc:mysql://localhost:3306/hwlogger?useSSL=false&serverTimezone=UTC")
      source.setUser("root")
      source.setPassword("root")
      source.setAllowPublicKeyRetrieval(true)
    source |> Option[MysqlDataSource]


final class DataSourceLoaderFactoryTests extends DataSourceLoaderFactory:
  override type Loader = DataSourceLoaderServiceTest

  override def apply(propertiesFile: String): DataSourceLoaderFactoryTests.this.Loader = DataSourceLoaderServiceTest(propertiesFile)



class HLDatabaseTest extends CommonHLDatabase:

  override def connect()(using context: HardwareLoggerTaskContext.WaitBackground): Either[HLDatabaseServiceException, HLDatabase] =
    import HLDatabaseService.given
    ox.either:
      val dbValue = HLDatabase(dbPropertiesFile)(using dataSourceLoaderFactory)
      dbValue match
        case Left(error) => error match
          case Error.ConnectionError => HLDatabaseServiceException.connectionError().fail()
          case _ => HLDatabaseServiceException.unknownError("An unknown error has occurred when connecting to the database.").fail()
        case Right(value) => value
  end connect

  override lazy val dbPropertiesFile: String =
    """
      |db.username=itsa
      |db.password=itsa123
      |db.name=itsaw
      |db.port=3306
      |db.unit7.address=10.0.1.230
      |db.unit9.address=192.168.100.230
      |db.local.address=127.0.0.1
      |
      |#Time in seconds
      |db.timeout=1
      |
      |db.autoReconnect=true
      |db.maxReconnects=3
      |db.useLocalhost=true
      |db.localhost.username=root
      |db.localhost.password=root
      |""".stripMargin
  override def markAllRowsWithIDInTableAsError[M <: HLModel : ClassTag](id: String)(using context: HardwareLoggerTaskContext.WaitBackground): Either[HLDatabaseServiceException, Unit] =
    super.markAllRowsWithIDInDBAsError(id)

end HLDatabaseTest
