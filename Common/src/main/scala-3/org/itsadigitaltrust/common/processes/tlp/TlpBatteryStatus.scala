package org.itsadigitaltrust.common.processes.tlp


case class TlpBatteryStatus(
                             @TlpProperty("charge") remainingJuice: Double,
                             capacity: Double,
                             status: String,
                             cycleCount: Option[Int],
                             voltage: Option[Double],
                             @TlpProperty("energy_full") designEnergy: Option[Double],
                             // Tracking current power usage / charge rate in milliWatts (mW)
                             @TlpProperty("power") powerDraw: Option[Double],
                             deviceModel: Option[String]
                           )