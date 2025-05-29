package org.itsadigitaltrust.common.processes

import org.itsadigitaltrust.common.OSUtils
import org.itsadigitaltrust.common.Operators.|>
import org.itsadigitaltrust.common.collections.{ApacheFuzzyMap, FuzzyMap}

import scala.annotation.tailrec

class Lsblk private:

  import org.itsadigitaltrust.common.Conversions.intToBool

  private type DiskInfo = (name: String, rota: Boolean)

  private var info: FuzzyMap[DiskInfo] = ApacheFuzzyMap()
  
    private[processes] def parse(output: String): FuzzyMap[DiskInfo] =
      @tailrec
      def repeatWhile2(input: String, cond: Char => Boolean, subsequentCond: Char => Boolean, result: String = ""): (result: String, rest: String) =
        if input.isBlank || input.isEmpty then
          (result, input)
        else
          val condResult = if  result.length > 1 then subsequentCond(input(0)) else cond(input(0))
          if !condResult then
            (result, input)
          else
            repeatWhile2(input.drop(1), cond, subsequentCond, result + input(0))
      end repeatWhile2
  
      def repeatWhile1(input: String, cond: Char => Boolean, result: String = "") =
        repeatWhile2(input, cond, cond, result)
  
      @tailrec
      def loop(seq: Seq[String], result: FuzzyMap[DiskInfo] =  ApacheFuzzyMap()): FuzzyMap[DiskInfo] =
        if seq.isEmpty then
          result
        else
          val name = repeatWhile2(seq.head, _.isLetter, _.isLetterOrDigit)
          val rota =  repeatWhile1(repeatWhile1(name.rest, _.isSpaceChar).rest, _.isDigit).result.toInt |> intToBool 
          loop(seq.drop(1), result ++ Map(name.result -> (name.result, rota)))
      end loop
      
      loop(output.split("\n").drop(1))
  end parse
  
  def apply()(using ProcessConfig): FuzzyMap[DiskInfo] =
    if OSUtils.onLinux then
      val result  = sudo"lsblk -d -o name,rota"
      info = parse(result)
      info
    else 
      info
  end apply
  
  def apply(devicePath: String): Option[DiskInfo] =
    info.getFirstValueForMatchingKey(devicePath.replaceFirst("/dev/", ""))
end Lsblk


object Lsblk:
  def apply()(using ProcessConfig): Lsblk =
    val lsblk = new Lsblk
    lsblk()
    lsblk


