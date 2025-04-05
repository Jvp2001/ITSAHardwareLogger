package org.itsadigitaltrust.hdsentinelreader

import java.io.{BufferedReader, InputStreamReader, OutputStream}
import java.nio.file.Paths
import java.util.Scanner
import scala.io.Source
import scala.util.Using

private[hdsentinelreader] object ProcessRunner:
  def apply(sudoPassword: String, output: XMLFile): String =

    if System.getProperty("os.name").toLowerCase.contains("linux") then
      val jarPath = Paths.get(getClass.getProtectionDomain.getCodeSource.getLocation.getPath).toFile
      if !jarPath.exists() then
        jarPath.mkdirs()
      val reportsPath = Paths.get(jarPath.toURI.toString, "reports").toFile
      if !reportsPath.exists() then
        reportsPath.mkdirs()

      val outputPath = Paths.get(jarPath.toPath.toString, "reports", output.toString)
      val runtime = Runtime.getRuntime
      val commands = Array("bash", "-c", s"echo $sudoPassword | sudo -S /usr/local/bin/HDSentinel -xml -dump")
      val process = runtime.exec(commands)
      Using(new Scanner(process.getInputStream).useDelimiter("\\A")): scanner =>
        if scanner.hasNext then scanner.next() else ""
      .get
    else
      scala.sys.error("This function will only work on Linux!")




