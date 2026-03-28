
ThisBuild / packageOptions += Package.ManifestAttributes(
  "Build-Time" -> java.time.Instant.now.toString
)
ThisBuild / normalizedName := "ITSA Hardware Logger"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "org.itsadigitaltrust"
ThisBuild / scalaVersion := "3.8.2"
ThisBuild / organizationName := "ITSA Digital Trust"
ThisBuild / scalaBinaryVersion := "3"
ThisBuild / organizationHomepage := Some(url("https://itsadigitaltrust.org/"))
ThisBuild / homepage := Some(url("https://github.com/jvp2001/ITSAHardwareLogger"))
ThisBuild / licenses += "GNUv3" -> url("https://www.gnu.org/licenses/gpl-3.0.en.html#license-text")

ThisBuild / buildInfoPackage := "org.itsadigitaltrust.hardwarelogger.core"
ThisBuild / buildInfoKeys := Seq(
  normalizedName,
  version,
  organizationName,
  scalaVersion,
  organizationHomepage)

ThisBuild / scalacOptions ++= Seq(
  "-groups",
  "-Xwiki-syntax",
  "-experimental"
)


fork := true

javaOptions ++= Seq()

enablePlugins(AssemblyPlugin, BuildInfoPlugin)
Compile / mainClass := Some("org.itsadigitaltrust.hardwarelogger.$HardwareLoggerApplication")

lazy val assemblyTargetOs = settingKey[String]("The name of the OS for the assembly")
ThisBuild / assemblyTargetOs := "linux"


// Allow java sources
//Global / onChangedBuildSource := IgnoreSourceChanges
ThisBuild / assemblyOutputPath := file(s"${(ThisBuild / assemblyTargetOs).value}/ITSAHardwareLogger.jar")

ThisBuild / assemblyMergeStrategy := {

  case PathList(ps@_*) if ps.last.endsWith(".dll") || ps.last.endsWith(".so") || ps.last.endsWith(".dylib") =>
    val target = (ThisBuild / assemblyTargetOs).value
    // Only keep the file if its path or filename matches our target OS string
    if (ps.exists(_.contains(target)) || ps.last.contains(target)) MergeStrategy.first
    else MergeStrategy.discard
  case PathList("META-INF", xs@_*) =>
    xs map
      {
        _.toLowerCase
      } match
    {
      case "services" :: xs =>
        MergeStrategy.filterDistinctLines

      case _ => MergeStrategy.discard
    }
  case _ => MergeStrategy.first
}

lazy val loggingDeps = Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.6",
  "ch.qos.logback" % "logback-classic" % "1.5.32"
)

val javafxVersion = "24" // Use your preferred version
val platforms = Seq("win", "linux", "mac")
val modules = Seq("base", "controls", "graphics", "web")

lazy val javaFXDeps = platforms
  .flatMap(platform =>
    modules
      .map(module => "org.openjfx" % s"javafx-$module" % javafxVersion classifier platform)
  )

lazy val scalaFXDeps = Seq(
  "org.scalafx" %% "scalafx" % "24.0.2-R36",
  "org.scalafx" %% "scalafx-extras" % "0.11.0",
)

lazy val uiDependencies = (javaFXDeps ++ scalaFXDeps)
  .map(_ withJavadoc() withSources())

lazy val root = (project in file("."))
  .settings(
    buildInfoKeys := Seq(name, version, organizationName, scalaVersion, organizationHomepage),
    //    buildInfoUsePackageAsPath := true,
    buildInfoPackage := "org.itsadigitaltrust.hardwarelogger.core",
    buildInfoOptions += BuildInfoOption.ToJson,
    assembly / assemblyJarName := "ITSAHardwareLogger.jar",
    assembly / mainClass := Some("org.itsadigitaltrust.hardwarelogger.HardwareLoggerApplication"),
    assembly / resourceDirectory := file("src/main/resources"),
    name := "ITSAHardwareLogger",


    libraryDependencies ++= uiDependencies,
    libraryDependencies += "com.github.oshi" % "oshi-core" % "6.10.0",
    libraryDependencies ++= commonDependencies,

    // addForegroundTask scalatest
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test,
    scalacOptions += "-experimental"

  ).dependsOn(common, backend, hdsentinelreader, issueReporter)


lazy val common = (project in file("Common"))
  .settings(
    name := "Common",
    libraryDependencies ++= commonDependencies
  )

lazy val commonDependencies = Seq(
  "com.augustnagro" %% "magnum" % "1.3.1",
  "com.mysql" % "mysql-connector-j" % "9.6.0",
  "org.apache.commons" % "commons-text" % "1.15.0",
  //  "org.checkerframework" % "checker" % "3.54.0",
  "com.softwaremill.ox" %% "core" % "1.0.4",
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-xml" % "2.21.1",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.21.1",
  "org.scala-lang.modules" %% "scala-xml" % "2.4.0",
  "com.somainer" %% "scala3-nameof" % "0.0.1" % Provided

) ++ loggingDeps.map(_ withJavadoc() withSources())

lazy val hdsentinelreader = (project in file("HDSentinelReader")).
  settings(
    name := "HDSentinelReader",

    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.dataformat" % "jackson-dataformat-xml" % "2.21.1",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.21.1",
      "org.scala-lang.modules" %% "scala-xml" % "2.4.0",
    ).map(_ withJavadoc() withSources()),
    scalacOptions += "-experimental"
  ).dependsOn(common)

lazy val backend = (project in file("Backend"))
  .settings(
    name := "Backend",
    libraryDependencies ++= commonDependencies,
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test,
    Test / unmanagedSourceDirectories += file("tests"),
  ).dependsOn(common)


lazy val issueReporter = (project in file("IssueReporter"))
  .settings(
    name := "IssueReporter",
    libraryDependencies ++= Seq(
      "org.kohsuke" % "github-api" % "1.330"
    ).map(_ withSources() withJavadoc())
  ).dependsOn(common)



// create an assembly task that assembly a jar for linux
commands += Command.single("assemblyFor"){(state, os) =>
  val extracted = Project.extract(state)
  // 1. Temporarily set the target OS
  val newState = extracted.appendWithSession(
    Seq(ThisBuild / assemblyTargetOs := os),
    state
  )
  // 2. Execute the assembly with the new setting
  "assembly" :: newState
  "exit" :: state
}
commands ++= Seq(
  Command.command("assembleForWindows") (state => "assemblyFor win" :: state),
  Command.command("assembleForMac") (state => "assemblyFor mac" :: state),
  Command.command("assembleForLinux") (state => "assemblyFor linux" :: state)
)