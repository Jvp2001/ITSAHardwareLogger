package org.itsadigitaltrust.hardwarelogger.core
import org.itsadigitaltrust.common.AssetLoader
import org.itsadigitaltrust.common.AssetLoader.assetsPath
import org.itsadigitaltrust.common.logging.HWLLogger

import scala.reflect.ClassTag
object UIAssetLoader:
  export AssetLoader.asset
  
  extension (sc: StringContext)
    inline def image(args: Any*)(using inline logger: HWLLogger): String =
      val filePath = sc.s(args*)
      asset"images/$filePath"

    inline def stylesheet(args: Any*)(using inline logger: HWLLogger): String =
      val filePath = sc.s(args *)
      asset"stylesheets/$filePath"
  end extension

end UIAssetLoader

