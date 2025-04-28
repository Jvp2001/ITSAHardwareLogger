package org.itsadigitaltrust.common.processes

import java.util.Scanner
import scala.util.Using

object ProcessRunner:
  private def apply(sudoPassword: String, command: String, requiresSudo: Boolean) =
    val fullCommand = if requiresSudo then s"echo $sudoPassword | sudo -S $command" else command
    val runtime = Runtime.getRuntime
    val process = runtime.exec(fullCommand.split(" "))
    Using(new Scanner(process.getInputStream).useDelimiter("\\A")): scanner =>
      if scanner.hasNext then scanner.next() else ""
    .get

  def apply(command: String): String =
    apply("", command, false)

  def apply(sudoPassword: String, command: String): String =
    apply(sudoPassword, command, true)
 
