package org.itsadigitaltrust.common



object ReflectionUtils:
  import scala.deriving.Mirror
  import scala.reflect.ClassTag

  inline def totalNumberOfChildClasses[T](using Mirror.SumOf[T]): Long =
    findSubclassModulesOfSealedTrait[T].size

  inline def findSubclassModulesOfSealedTrait[T](using m: Mirror.SumOf[T]):List[ClassTag[? <: T]] =
    allInstances[m.MirroredElemTypes, m.MirroredType]

  private inline def allInstances[ET <: Tuple, T]: List[ClassTag[? <: T]] =
    import scala.compiletime.*

    inline erasedValue[ET] match
      case _: EmptyTuple => Nil
      case _:(t *: ts) => summonInline[ClassTag[t]].asInstanceOf[ClassTag[? <:T]] :: allInstances[ts, T]
      