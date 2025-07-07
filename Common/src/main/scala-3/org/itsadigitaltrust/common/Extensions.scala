package org.itsadigitaltrust.common

import org.itsadigitaltrust.common.types.DataSizeType.{DataSize, DataSizeUnit}
import org.itsadigitaltrust.common.types.FrequencyType.{Frequency, FrequencyUnit}

import java.io.InputStream
import java.net.URI
import scala.annotation.{tailrec, targetName}
import scala.reflect.{ClassTag, classTag}
import scala.util.{Try, Using}

extension [T](coll: IndexedSeq[T])
  def filterByIndices(predicate: Int => Boolean): Seq[T] =
    coll.zipWithIndex.filter: (*, index) =>
      predicate(index)
    .map: (item, *) =>
      item

  def getEvenIndexItems: Seq[T] =
    filterByIndices: index =>
      index % 2 == 0

  def getOddIndexItems: Seq[T] =
    filterByIndices: index =>
      index % 2 != 0
end extension


object Maths:


  extension (long: Double)
    @targetName("pow")
    infix def **(raiseBy: Double): Double =
      Math.pow(long, raiseBy)
    def KiB: DataSize = DataSize(long, DataSizeUnit.KiB)
    def MiB: DataSize = DataSize(long, DataSizeUnit.MiB)
    def GiB: DataSize = DataSize(long, DataSizeUnit.GiB)
    def TiB: DataSize = DataSize(long, DataSizeUnit.TiB)
    def PiB: DataSize = DataSize(long, DataSizeUnit.PiB)
    def KB: DataSize = DataSize(long, DataSizeUnit.KB)
    def MB: DataSize = DataSize(long, DataSizeUnit.MB)
    def GB: DataSize = DataSize(long, DataSizeUnit.GB)
    def TB: DataSize = DataSize(long, DataSizeUnit.TB)
    def PB: DataSize = DataSize(long, DataSizeUnit.PB)
    def B: DataSize = DataSize(long, DataSizeUnit.B)


    def Hz: Frequency = Frequency(long, FrequencyUnit.Hz)
    def kHz: Frequency = Frequency(long, FrequencyUnit.kHz)
    def MHz: Frequency = Frequency(long, FrequencyUnit.MHz)

end Maths

export Maths.*


object StringExtensions:
  extension (s: String)
    def startWithIgnoreCase(string: String): Boolean =
      s.toLowerCase.startsWith(string)

    inline def loadAsResource[T : ClassTag]: Try[URI] =
      Try(classTag[T].runtimeClass.getResource(s).toURI)
end StringExtensions

export StringExtensions.*

object InputStreamExtensions:
  extension(is: InputStream)
    def readAllAsString(): String =
      Using(is): _ =>
        scala.io.Source.fromInputStream(is).getLines().mkString("\n")
      .get
end InputStreamExtensions

export InputStreamExtensions.*


object TryExtensions:
  extension[T](`try`: Try[Option[T]])
    def unwrapSafe: Option[T] =
      `try`.getOrElse(None)

    def toOptionFlat: Option[T] =
      `try`.toOption.flatten
  end extension

end TryExtensions


export TryExtensions.*