package org.itsadigitaltrust.common

import java.net.URL
import scala.reflect.{ClassTag, classTag}

object Interpolators:
  extension (sc: StringContext)
    def resource[C : ClassTag](args: Any*): URL =
      classTag[C].runtimeClass.getResource(sc.s(args))  
