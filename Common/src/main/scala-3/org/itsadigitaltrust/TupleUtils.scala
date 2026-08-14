package org.itsadigitaltrust



object TupleUtils:
  trait Mapper[F[_]]:
    def apply[T](x: T): F[T]

  inline def mapTuple[T <: Tuple, F[_]](t: T)(mapper: => [X] => X => F[X]): Tuple.Map[T, F] =
    inline t match
      case cons: (h *: t) =>
        // Compute head and recurse on tail at compile time
        val headMapped = mapper(cons.head)
        val tailMapped = mapTuple(cons.tail)(mapper)
        (headMapped *: tailMapped).asInstanceOf[Tuple.Map[T, F]]
      case _: EmptyTuple => EmptyTuple.asInstanceOf[Tuple.Map[T, F]]
  end mapTuple
end TupleUtils


