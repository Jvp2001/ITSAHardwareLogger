package org.itsadigitaltrust.hdsentinelreader

import java.io.{BufferedReader, InputStreamReader, OutputStream}
import scala.io.Source
import scala.util.Using

private[hdsentinelreader] object ProcessRunner:
  def apply(sudoPassword: String, output: XMLFile) =

    if System.getProperty("os.name").toLowerCase.contains("linux") then
      val runtime = Runtime.getRuntime
      val commands = Array("bash", "-c", s"echo $sudoPassword | sudo -S /usr/local/bin/HDSentinel -xml -r $output")
      val process = runtime.exec(commands)
      process.waitFor()
      Using(Source.fromFile(output.toString))
      
    else
      scala.sys.error("This function will only work on Linux!")


  private def getOutputString(process: Process): String =
    val stringBuilder = new StringBuilder()
    Using(new BufferedReader(new InputStreamReader(process.getInputStream))): bf =>
      var line: String = ""
      while {
        line = bf.readLine(); line != null
      } do
        stringBuilder ++= line
    stringBuilder.toString()




