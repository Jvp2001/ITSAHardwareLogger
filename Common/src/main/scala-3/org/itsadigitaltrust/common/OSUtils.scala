package org.itsadigitaltrust.common

object OSUtils:
  private lazy val osName: String = System.getProperty("os.name").toLowerCase
  
  lazy val onWindows: Boolean = osName.startsWith("windows")
  lazy val onMacos: Boolean = osName.startsWith("mac")
  lazy val onLinux: Boolean = osName.startsWith("linux")
  lazy val onOther: Boolean = !(onWindows || onMacos || onLinux)
  
  def getOSName: "Windows" | "macOS" | "Linux" | "Other" =
    if onWindows then "Windows"
    else if onMacos then "macOS"
    else if onLinux then "Linux"
    else "Other"
