package org.itsadigitaltrust.common.processes.tlp

import org.itsadigitaltrust.common.processes.tlp.{TlpBatteryStatus, TlpDecoder}

import scala.sys.process.*

private[tlp] object TlpParser:

  // 1. Define our Type Literal Union of valid target components
  type TlpTarget = "status" | "battery" | "processor" | "usb" | "disk" | "pcie"

  /**
   * Universal fetcher function that converts a Type Literal into the correct tlp-stat flag,
   * runs the functional pipeline, and returns the parsed nested Map.
   */
  def fetch[A](target: TlpTarget)(using decoder: TlpDecoder[A]): Option[A] =
    // Map our type literal choices cleanly to the actual terminal flags
    val flag = target match
      case "status"    => "-s"
      case "battery"   => "-b"
      case "processor" => "-p"
      case "usb"       => "-u"
      case "disk"      => "-d"
      case "pcie"      => "-e"

    val command = Seq("sudo", "tlp-stat", flag, "-v")
    val lines = try command.lazyLines.toList catch case _: Exception => Nil

    // The clean functional parser engine
    val initialFoldState: (String, Map[String, Map[String, String]]) = ("Global", Map.empty)

    val (_, finalDataMap) = lines.foldLeft(initialFoldState):
      case ((currentSection, accumulator), rawLine) =>
        val line = rawLine.trim
        if line.isEmpty then
          (currentSection, accumulator)
        else if line.startsWith("+++") || line.startsWith("---") then
          val nextSection = line.replaceAll("^[+\\- ]+", "").replaceAll("[+\\- ]+$", "").trim
          (nextSection, accumulator)
        else
          val delimiterIdx = line.indexWhere(c => c == '=' || c == ':')
          if delimiterIdx != -1 then
            val key = line.substring(0, delimiterIdx).trim
            val value = line.substring(delimiterIdx + 1).trim
            val updatedInnerMap = accumulator.getOrElse(currentSection, Map.empty) + (key -> value)
            (currentSection, accumulator + (currentSection -> updatedInnerMap))
          else
            (currentSection, accumulator)


    val flattenedProperties = finalDataMap.values.foldLeft(Map.empty[String, String])(_ ++ _)
    decoder.decode(flattenedProperties)
  end fetch



@main def testTlpMacroSuite(): Unit =
  println("==================================================")
  println("🧪 RUNNING TLP VERBOSE MACRO DERIVATION TESTS")
  println("==================================================")

  // -------------------------------------------------------------------------
  // Test 1: Simulating Verbose Output (Desktop/CI Safe Simulation)
  // -------------------------------------------------------------------------
  println("\n[Test 1] Simulating Mock Verbose Battery Output Data...")

  // This mimics what the text parser flattens from 'tlp-stat -b -v'
  val mockSystemProperties = Map(
    "charge" -> "84.5 [%]",
    "capacity" -> "92.1 [%]",
    "/sys/class/power_supply/bat0/status" -> "Discharging",
    "/sys/class/power_supply/bat0/cycle_count" -> "142",
    "/sys/class/power_supply/bat0/voltage_now" -> "11450000 [mV]", // Will translate to 11450000.0 Double
    "energy_full" -> "45000 [mWh]",
    "deviceModel" -> "ThinkPad Battery X1"
  )

  // Explicitly summon our macro decoder instance to test translation logic
  val batteryDecoder = summon[TlpDecoder[TlpBatteryStatus]]

  batteryDecoder.decode(mockSystemProperties) match
    case Some(battery) =>
      println("  ✅ Success! Macro successfully decoded mock properties:")
      println(s"    - Custom Property (remainingJuice) : ${battery.charge} %")
      println(s"    - Standard Property (capacity)      : ${battery.capacity} %")
      println(s"    - Status String                     : ${battery.status}")
      println(s"    - Option[Int] (cycleCount)          : ${battery.cycleCount}")
      println(s"    - Option[Double] (voltage)          : ${battery.voltage}")
      println(s"    - Option[Double] (designEnergy)     : ${battery.designEnergy}")
      println(s"    - Option[String] (deviceModel)      : ${battery.deviceModel}")
    case None =>
      println("  ❌ Test Failed: Macro was unable to cleanly parse mock properties.")

  // -------------------------------------------------------------------------
  // Test 2: Incomplete Data Field Recovery Test
  // -------------------------------------------------------------------------
  println("\n[Test 2] Testing Missing Optional Fields Fallback...")

  // This simulation strips away deviceModel and cycle_count completely
  val minimalMockProperties = Map(
    "charge" -> "100.0 [%]",
    "capacity" -> "100.0 [%]",
    "status" -> "Full"
  )

  batteryDecoder.decode(minimalMockProperties) match
    case Some(battery) =>
      println("  ✅ Success! Required properties parsed, and missing fields naturally set to None:")
      println(s"    - charge       : ${battery.charge}")
      println(s"    - cycleCount   : ${battery.cycleCount}") // Will output None
      println(s"    - deviceModel  : ${battery.deviceModel}") // Will output None
    case None =>
      println("  ❌ Test Failed: Missing parameters caused macro decoder to crash.")

  // -------------------------------------------------------------------------
  // Test 3: Hardware Diagnostics Execution
  // -------------------------------------------------------------------------
  println("\n[Test 3] Attempting Live Local Host System Scan...")

  TlpParser.fetch[TlpBatteryStatus]("battery") match
    case Some(liveBat) =>
      println("  🎉 Live Scan Success!")
      println(s"    - Current Battery Charge Level: ${liveBat.charge}%")
      println(s"    - Physical Hardware Condition: ${liveBat.capacity}% Health")
      println(s"    - System Power Source Mode   : ${liveBat.status}")
    case None =>
      println("  ℹ️ Live Scan Skipped/Unavailable (Expected if running on a desktop, server, or without tlp-stat installed).")

  println("\n==================================================")
  println("🏁 TEST EXECUTION FINISHED")
  println("==================================================")


// ❌ Try uncommenting the line below:
// val customInfo = fetch("graphics")
// Scala 3 will throw a compile error: "Found: (\"graphics\" : String), Required: TlpParser.TlpTarget"
