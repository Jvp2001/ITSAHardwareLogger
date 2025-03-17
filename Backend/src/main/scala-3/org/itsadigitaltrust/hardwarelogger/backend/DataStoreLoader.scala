package org.itsadigitaltrust.hardwarelogger.backend

import com.mysql.cj.jdbc.MysqlDataSource

import java.io.{File, FileInputStream}
import java.net.URI
import java.util.Properties
import javax.sql.DataSource
import scala.util.{Using, boundary}

object DataStoreLoader:
  enum Error:
    case PropertyNotFound(expected: String, got: String)

  import scala.compiletime.*
  opaque type PropertyFileName = String
  object PropertyFileName:
    inline def apply(file: String): PropertyFileName =
      inline if file == ""  || file == null then error(codeOf(file) + " cannot be empty or null!")
//      else if file.split("/")(0) != "db.properties" then error (codeOf(file) + " must end with .properties!")
      else file



  private type Prefix = "MYSQL" | "ORACEL"
  private type DataSourceType[P <: Prefix] = Prefix match
    case "MYSQL" => MysqlDataSource

  transparent inline private def createDataSource[P <: Prefix](prefix: Prefix): Any =
    prefix match
      case "MYSQL" => new MysqlDataSource()
      case "ORACEL" => error("Oracle is not supported yet!")


  def trySetProperty[T](name: String, mysqlDataSource: MysqlDataSource, props: Properties, setter: (MysqlDataSource, T) => Unit)(using label: boundary.Label[Error]): Unit =
    if !props.containsKey(name) then
      val error = Error.PropertyNotFound(name, name)
      boundary.break[Error](error)
    else
      setter(mysqlDataSource, props.get(name).asInstanceOf[T])


  def apply(configFile: URI)(using label: boundary.Label[Error]): MysqlDataSource =
    val dataSource = MysqlDataSource()
    val file = new File(configFile)
    Using(new FileInputStream(file)): fis =>
      val props = new Properties()
      println(fis.getFD)
      props.load(fis)

      dataSource.setURL(props.get("URL").toString)
      dataSource.setUser(props.get("USERNAME").toString)
      dataSource.setPassword(props.get("PASSWORD").toString)
      dataSource.setPort(props.get("PORT").toString.toInt)
      println(dataSource.getURL)
      println(dataSource.getUser)
      println(dataSource.getPassword)
      println(dataSource.getPort)


    dataSource




