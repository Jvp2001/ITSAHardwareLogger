package org.itsadigitaltrust.hardwarelogger.core

import scala.reflect.{ClassTag, classTag}


extension(sc: StringContext)
  inline def assets(args: Any*): String =
    val string = sc.s(args)
    getClass.getResource(s"assets${if string.startsWith("/") then "" else "/"}$string").toString

  @throws[NullPointerException]
  inline def stylesheet[T <: ClassTag](): String =
    getClass.getClassLoader.getResource(s"org/itsadigitaltrust/hardwarelogger/views/stylesheets/${sc.s}").toExternalForm