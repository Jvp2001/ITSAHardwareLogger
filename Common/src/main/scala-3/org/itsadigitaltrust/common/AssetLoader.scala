package org.itsadigitaltrust.common

import org.itsadigitaltrust.common.Operators.?
import org.itsadigitaltrust.common.logging.HWLLogger

object AssetLoader:

  final val assetsPath = "/assets/"

  extension (sc: StringContext)
    inline def asset(inline args: Any*)(using inline logger: HWLLogger): String =
      val fullPath = sc.s(args*)
      val assetPath = s"/assets/$fullPath"

      val classLoader = Thread.currentThread.getContextClassLoader
      classLoader.getResource(assetPath.stripPrefix("/")).?
        .map(_.toExternalForm)
        .getOrElse:
          logger.warn(s"Cannot find asset: '$assetPath'!")
          ""
    end asset
    
    inline def service(inline args: Any*)(using inline logger: HWLLogger): String =
      val fullPath = sc.s(args*)
      val servicesPath = asset"services/$fullPath"
      servicesPath
      
end AssetLoader
