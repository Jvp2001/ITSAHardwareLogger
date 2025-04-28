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
type StringImmutableSetter[M] = ImmutableSetter[M, String]
type IntImmutableSetter[M] = ImmutableSetter[M, Int]
type LongImmutableSetter[M] = ImmutableSetter[M, Long]
type FloatImmutableSetter[M] = ImmutableSetter[M, Float]
type DoubleImmutableSetter[M] = ImmutableSetter[M, Double]
type BooleanImmutableSetter[M] = ImmutableSetter[M, Boolean]