package org.itsadigitaltrust.common.processes

import scala.annotation.static

object RootChecker:
  lazy val isRunningAsRoot: Boolean =
    val userName = System.getProperty("user.name")

    val isRootID =
      ProcessHandle.current().info().user.map: u =>
        // On Linux, the string often looks like "root (0)" or just "0"
        // We can check if it equals "0" or contains "(0)"
        u
      .orElse("unknown").contains("0")

    isRootID || userName == "root"
    
      
