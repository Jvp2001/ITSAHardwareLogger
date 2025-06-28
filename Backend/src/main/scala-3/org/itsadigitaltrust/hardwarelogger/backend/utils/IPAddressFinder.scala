package org.itsadigitaltrust.hardwarelogger.backend.utils

import org.itsadigitaltrust.common.Operators.|>
import org.itsadigitaltrust.common.collections.ApacheFuzzyMap

import org.apache.commons.text.similarity.FuzzyScore

import java.net.{Inet4Address, InterfaceAddress, NetworkInterface}

object IPAddressFinder:

  import scala.jdk.CollectionConverters.*

  private def networkInterfaces = NetworkInterface.getNetworkInterfaces.asScala

  /**
   * Finds all IPv4 addresses on all ethernet cards.
   *
   * @param startsWithAny A list of address to check against.
   * @return The found addresses
   */
  //  def findIPv4Addresses(startsWithAny: String*): Seq[Option[(String, String)]] =
  //    networkInterfaces.filter(_.getName.startsWith("en")).filter(_.isUp).flatMap: inet =>
  //      val addresses = inet.getInetAddresses.asScala.filter(_.isInstanceOf[Inet4Address]).toSeq
  //      val result = addresses.map: address =>
  //        val addr = address.toString.split('.').dropRight(1).mkString(".").stripPrefix("/")
  //        val addressPair =
  //          startsWithAny.map: str =>
  //            val result = str.contains(addr)
  //            if result then
  //              Some(str -> addr)
  //            else
  //              None
  //          .find(_.isDefined)
  //          .flatten
  //        end addressPair
  //
  //        if addressPair.isDefined && !(address.isLoopbackAddress && address.isMulticastAddress && address.isLinkLocalAddress) then
  //          addressPair
  //        else
  //          None
  //      end result
  //      result
  //    .toSeq
  //  end findIPv4Addresses

  // use ApacheFuzzyMap to find the best matching address
  def findDatabaseAddress(addresses: String*): Option[String] =

    val fuzzyMap = ApacheFuzzyMap[String]()
    val inetAddresses = networkInterfaces.filter(_.getName.startsWith("en")).filter(_.isUp).flatMap: inet =>
      inet.getInetAddresses.asScala.filter(_.isInstanceOf[Inet4Address]).map: address =>
        val foundAddress = address.toString.stripPrefix("/")
        println(foundAddress)
        foundAddress


    val fuzzyScore = new FuzzyScore(java.util.Locale.UK)
    inetAddresses.flatMap: address =>
      addresses.map: addr =>
        val score = fuzzyScore.fuzzyScore(address, addr)
        val result = (addr, address) -> score.toInt
        println(result)
        result
    .toSeq.sortBy(_._2).lastOption.match
      case Some(value) => value._1._1
      case _ => null
    |> Option[String]

end IPAddressFinder






