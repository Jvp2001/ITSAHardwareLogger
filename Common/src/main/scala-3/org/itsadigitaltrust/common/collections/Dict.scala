package org.itsadigitaltrust.common.collections


class Dict(private var unsafeRawMap: Map[String, ?] = Map.empty) extends Selectable:
  transparent inline def apply(inline key: String): Any = unsafeRawMap(key)
  transparent inline def selectDynamic(inline key: String): Any = unsafeRawMap(key)
  transparent inline def applyDynamic(inline key: String)(inline value: Any): Dict =
    unsafeRawMap = unsafeRawMap.updated(key, value)
    this

  def ++[D2 <: Dict](rhs: D2 ): this.type & D2 =
    new Dict(unsafeRawMap ++ rhs.unsafeRawMap).asInstanceOf[this.type & D2]
end Dict

object Dict:
  transparent inline def fromUnsafeMaps(maps: Map[String, ?]*) =
    new Dict(maps.reduce(_ ++ _))

  def fromUnsafeMap(unsafeRawMap: Map[String, ?]): Dict =
    new Dict(unsafeRawMap)

  transparent inline def apply(inline block: => Unit): Dict =
    import org.itsadigitaltrust.common.macros.DictMacros
    DictMacros(block)
end Dict
