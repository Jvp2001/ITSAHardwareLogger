package org.itsadigitaltrust.hardwarelogger.mvvm.properties.accessorfunctions


type Getter[T, M] = M => T
type StringGetter[M] = Getter[String, M]
type IntGetter[M] = Getter[Int, M]
type LongGetter[M] = Getter[Long, M]
type FloatGetter[M] = Getter[Float, M]
type DoubleGetter[M] = Getter[Double, M]




