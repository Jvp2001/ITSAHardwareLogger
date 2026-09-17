package org.itsadigitaltrust.common.processes.tlp

object TlpStat:

  /**
   * Fetches the current decoded status metrics of the system battery.
   */
  def getBatteryStatus: Option[TlpBatteryStatus] =
    TlpParser.fetch[TlpBatteryStatus]("battery")

  /**
   * Verifies if the device is currently discharging power (running off the battery).
   *
   * @return true if the system status is explicitly "Discharging".
   *         false if the system is plugged in (Charging/Full) or if no battery data is available.
   */
  def isRunningOffBattery: Boolean =
    getBatteryStatus.exists: battery =>
      battery.status.trim.toLowerCase == "discharging"

  /**
   * Helper utility to confirm if battery hardware is detected on the host system.
   * Useful to prevent false-negative checks on desktop towers or server instances.
   */
  def hasBattery: Boolean =
    getBatteryStatus.isDefined
end TlpStat
