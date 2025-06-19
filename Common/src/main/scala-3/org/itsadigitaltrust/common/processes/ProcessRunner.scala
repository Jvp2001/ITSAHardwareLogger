package org.itsadigitaltrust.common.processes

import scala.sys.process.*


extension (sc: StringContext)
  inline def sudo(args: String*)(using config: ProcessConfig): String =
    val sudoPasswordInputCommand = s"echo ${config.sudoPassword} | sudo -S "
    val input =
      val res = sc.s(args*)
      if !res.startsWith(sudoPasswordInputCommand) then
        f"$sudoPasswordInputCommand $res"
      else
        res
    end input
    println(s"input starts with sudo: ${input.startsWith(sudoPasswordInputCommand)}")
    println(s"input: $input")
    val parts = input.split("\\|")
    (parts.head #| parts.tail.mkString).!!
  end sudo
  inline def proc(args: String*): String =
    val input = sc.s(args*)
    input.!!
  end proc

end extension

