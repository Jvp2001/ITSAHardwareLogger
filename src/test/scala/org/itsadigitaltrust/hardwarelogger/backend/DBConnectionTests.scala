package org.itsadigitaltrust.hardwarelogger.backend

import org.itsadigitaltrust.hardwarelogger.backend.DataStoreLoader.PropertyFileName
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.funsuite.AnyFunSuite

import scala.util.boundary
import com.augustnagro.magnum.*
import org.itsadigitaltrust.hardwarelogger.backend.entities.Hdd


class DBConnectionTests extends AnyFunSuite:
  test("DB Connection is Successful"):
    boundary:
      val dataStore = DataStoreLoader(getClass.getResource("db.properties").toURI)
      println(dataStore)
      assert(dataStore != null)
      given xa: Transactor = Transactor(dataStore)
      println(dataStore.getUser)
      val disks = connect(dataStore):
        sql"SELECT * from mysql.disks limit 20".query[Hdd].run()
      println(disks)

