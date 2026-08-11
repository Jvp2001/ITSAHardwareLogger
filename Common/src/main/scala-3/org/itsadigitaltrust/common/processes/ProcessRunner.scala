package org.itsadigitaltrust.common.processes

import org.itsadigitaltrust.common.Operators.|>
import org.itsadigitaltrust.common.logging.{HWLLoggable, HWLLogger, HWLLoggerImpl}

import scala.sys.process.*
import scala.util.Try

final class SudoError(message: String, cause: Throwable) extends Throwable(message, cause)


extension (sc: StringContext)
  private def handleResult(res: => String): Either[SudoError, String] =
    Try(res).toEither
      .left.map: error =>
        SudoError(error.getMessage, error)
        
  def sudo(args: String*)(using config: ProcessConfig, logger: HWLLoggable): Either[SudoError, String] =
    val sudoPasswordInputCommand = s"echo ${config.sudoPassword} | sudo -S "
    val input =
      val res = sc.s(args*)
      if !res.startsWith(sudoPasswordInputCommand) then
        f"$sudoPasswordInputCommand $res"
      else
        res
    end input
    logger().info(s"input starts with sudo: ${input.startsWith(sudoPasswordInputCommand)}")
    logger().info(s"input: $input")
    val parts = input.split("\\|")
    handleResult((parts.head #| parts.tail.mkString).!!)
  end sudo


end extension

