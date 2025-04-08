package org.itsadigitaltrust.common

object OSUtils:
  lazy val osName: String = System.getProperty("os.name").toLowerCase
  
  lazy val onWindows: Boolean = osName.startsWith("windows")
  lazy val onMacos: Boolean = osName.startsWith("mac")
  lazy val onLinux: Boolean = osName.startsWith("linux")
  
  lazy val onOther = !(onWindows || onMacos || onLinux)
