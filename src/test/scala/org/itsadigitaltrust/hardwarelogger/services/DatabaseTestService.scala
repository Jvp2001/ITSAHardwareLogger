package org.itsadigitaltrust.hardwarelogger.services

import com.augustnagro.magnum.sql
import org.itsadigitaltrust.common.Operators.|>
import org.itsadigitaltrust.hardwarelogger.backend.database.HLDatabase
import org.itsadigitaltrust.hardwarelogger.backend.{DataSourceLoaderService, HLSqlDataSource}
import org.itsadigitaltrust.hardwarelogger.models.HLModel
import org.itsadigitaltrust.hardwarelogger.shared.HardwareLoggerSharedData
import ox.either.ok

import scala.reflect.ClassTag


class DatabaseTestService(using localhost: Boolean = false, backend: HLDatabase, dataSourceLoaderService: DataSourceLoaderService[HLSqlDataSource]) extends MySQLHardwareLoggerDatabaseService(using localhost, backend)
//
//  def saveData(sharedData: HardwareLoggerSharedData)(using HardwareGrabberService): Either[Error, Unit] =
//    ox.either:
//      sharedData.drives.foreach(this.insertAll)
//      sharedData.processors.foreach(this.insertAll)
//      sharedData.memory.foreach(this.insertAll)
//      sharedData.media.foreach(this.insertAll)
//      sharedData.media.foreach(this.insertAll)

//
//  def truncateAllTables(): HLDBTransaction[Unit] =
//    transact: dbTx ?=>
//      Seq("info", "memory", "disks", "media", "wiping").foreach: table =>  
//        sql"truncate $table".update.run()
//      Right(())

export DatabaseTestService.given