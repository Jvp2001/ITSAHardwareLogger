package org.itsadigitaltrust.common

import java.net.{InetAddress, InetSocketAddress, Socket, URI, URL, URLConnection}
import scala.util.Using

object NetworkUtils:
  def isConnected: Boolean =
    Using(Socket()): socket =>
      val inetSocket = new InetSocketAddress("https://google.com", 80)
      socket.connect(inetSocket, 250)
    .isSuccess



