package org.itsadigitaltrust.hardwarelogger.mvvm.properties.accessorfunctions

type Setter[R, M, V] = (M, V) => R
type MutableSetter[M, V] = Setter[Unit, M, V]
type StringSetter[M] = MutableSetter[M, String]
type IntSetter[M] = MutableSetter[M, Int]
type LongSetter[M] = MutableSetter[M, Long]
type FloatSetter[M] = MutableSetter[M, Float]
type DoubleSetter[M] = MutableSetter[M, Double]
type BooleanSetter[M] = MutableSetter[M, Boolean]


type ImmutableSetter[M, V] = Setter[M, M, V]
type ImmutableStringSetter[M] = ImmutableSetter[M, String]
type ImmutableIntSetter[M] = ImmutableSetter[M, Int]
type ImmutableLongSetter[M] = ImmutableSetter[M, Long]
type ImmutableFloatSetter[M] = ImmutableSetter[M, Float]
type ImmutableDoubleSetter[M] = ImmutableSetter[M, Double]
type ImmutableBooleanSetter[M] = ImmutableSetter[M, Boolean]