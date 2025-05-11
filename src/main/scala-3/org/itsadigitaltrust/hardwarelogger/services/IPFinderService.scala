package org.itsadigitaltrust.hardwarelogger.services

import org.itsadigitaltrust.common.Result

import java.net.{Inet6Address, InetAddress, InterfaceAddress, NetworkInterface}
import scala.annotation.tailrec
import scala.collection.JavaConverters.enumerationAsScalaIteratorConverter
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.Success


trait IPFinderService:
  enum IPFinderServiceError:
    case IPWithPrefixNotFound(ip: String)
    case SocketError(msg: String)
    case NoIPAddressesFound
    case NoInterfacesFound

  type ![T] = Result.Continuation[T, IPFinderServiceError] ?=> T

  def findAddresses(): Vector[InterfaceAddress]


  def error(error: IPFinderServiceError): ![Nothing] =
    Result.error(error)


object IPFinderService:
  given IPv4FinderService: IPFinderService = () =>
    val interfaces: Iterator[NetworkInterface] = NetworkInterface.getNetworkInterfaces.asScala.iterator
    interfaces.flatMap: interface =>
      if interface.isUp && !interface.isLoopback then
        interface.getInterfaceAddresses.asScala.filterNot(_.getAddress.isInstanceOf[Inet6Address])
      else
        Vector()
    .toVector




