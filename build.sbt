import sbt.Keys.libraryDependencies
import sbtbuildinfo.BuildInfoKeys

import java.time.{Clock, LocalDateTime, ZoneId, ZoneOffset}

ThisBuild / packageOptions += Package.ManifestAttributes(
  "Build-Time" -> java.time.Instant.now.toString
)
ThisBuild / normalizedName := "ITSA Hardware Logger"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "org.itsadigitaltrust"
ThisBuild / scalaVersion := "3.7.0"
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



enablePlugins(AssemblyPlugin, BuildInfoPlugin)

Compile / mainClass := Some("org.itsadigitaltrust.hardwarelogger.$HardwareLoggerApplication")

// Allow java sources
//Global / onChangedBuildSource := IgnoreSourceChanges
ThisBuild / assemblyOutputPath := file("ITSAHardwareLogger.jar")
ThisBuild / assemblyMergeStrategy := {
  case PathList("META-INF", xs@_*) =>
    xs map {_.toLowerCase} match {
      case "services" :: xs =>
        MergeStrategy.filterDistinctLines
      case _ => MergeStrategy.discard
    }
  case _ => MergeStrategy.first
}

lazy val loggingDeps = Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "ch.qos.logback" % "logback-classic" % "1.5.18"
)

lazy val javaFXDeps = Seq("linux").flatMap { osName =>
  Seq("base", "controls", "graphics", "web")
    .map(m => "org.openjfx" % s"javafx-$m" % "21" classifier osName)
}

lazy val scalaFXDeps = Seq(
  "org.scalafx" %% "scalafx" % "24.0.2-R36",
  "org.scalafx" %% "scalafx-extras" % "0.11.0",
)

lazy val uiDependencies = (javaFXDeps ++ scalaFXDeps)
  .map(_ withJavadoc() withSources())
enablePlugins()

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
    libraryDependencies += "com.github.oshi" % "oshi-core" % "6.8.2",
    libraryDependencies ++= commonDependencies,

    // add scalatest
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test,
    scalacOptions += "-experimental"

  ).dependsOn(common, backend, hdsentinelreader, issueReporter)


lazy val common = (project in file("Common"))
  .settings(
    name := "Common",
    libraryDependencies ++= commonDependencies ++ loggingDeps ++ Seq (
      "com.fasterxml.jackson.dataformat" % "jackson-dataformat-xml" % "2.19.1",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.19.1",
      "org.scala-lang.modules" %% "scala-xml" % "2.4.0",
    ).map(_ withSources() withJavadoc())
  )

lazy val commonDependencies = Seq(
  "com.augustnagro" %% "magnum" % "1.3.1",
  "com.mysql" % "mysql-connector-j" % "9.3.0",
  "org.apache.commons" % "commons-text" % "1.13.1",
  "org.checkerframework" % "checker" % "3.49.5"
).map(_ withJavadoc() withSources())

lazy val hdsentinelreader = (project in file("HDSentinelReader")).
  settings(
    name := "HDSentinelReader",

    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.dataformat" % "jackson-dataformat-xml" % "2.19.1",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.19.1",
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
      "org.kohsuke" % "github-api" % "1.327"
    ).map(_ withSources() withJavadoc())
  ).dependsOn(common)




ThisBuild / scalacOptions ++= Seq(
  "-groups",
  "-Xwiki-syntax",
  "-experimental"
)

