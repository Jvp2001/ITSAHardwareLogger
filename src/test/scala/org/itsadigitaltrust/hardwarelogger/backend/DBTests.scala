package org.itsadigitaltrust.hardwarelogger.backend

import org.itsadigitaltrust.hardwarelogger.backend.DataStoreLoader.PropertyFileName
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.funsuite.AnyFunSuite

import scala.util.boundary
import com.augustnagro.magnum.*
import org.itsadigitaltrust.hardwarelogger.backend.entities.{Info, Wiping}


class DBTests extends AnyFunSuite:
  case class HardwareInfo(info: Info, wiping: Wiping)
  def getHardwareData: Unit = ()


