import sbt.Keys.libraryDependencies


ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.7.1"
Compile / mainClass := Some("org.itsadigitaltrust.hardwarelogger.$HardwareLoggerApplication")

// Allow java sources

ThisBuild / assemblyOutputPath := file("ITSAHardwareLogger.jar")
ThisBuild / assemblyMergeStrategy := {
  case PathList("META-INF", _*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}

lazy val javaFXDeps = Seq("win", "mac", "linux").flatMap { osName =>
  Seq("base", "controls", "graphics")
    .map(m => "org.openjfx" % s"javafx-$m" % "23" classifier osName)
}

lazy val scalaFXDeps = Seq(
  "org.scalafx" %% "scalafx" % "23.0.1-R34",
  "org.scalafx" %% "scalafx-extras" % "0.11.0",
)

lazy val uiDependencies = (javaFXDeps ++ scalaFXDeps)
  .map(_ withJavadoc() withSources())


lazy val root = (project in file("."))
  .settings(
    assembly / assemblyJarName := "ITSAHardwareLogger.jar",
    assembly / mainClass := Some("org.itsadigitaltrust.hardwarelogger.HardwareLoggerApplication"),
    assembly / resourceDirectory := file("src/main/resources"),
    name := "ITSAHardwareLogger",


    libraryDependencies ++= uiDependencies,
    libraryDependencies += "com.github.oshi" % "oshi-core" % "6.8.2",
    libraryDependencies ++= commonDependencies,
    scalacOptions += "-experimental"

  ).dependsOn(common, backend, hdsentinelreader, issueReporter).aggregate()


lazy val common = (project in file("Common"))
  .settings(
    name := "Common",
    libraryDependencies ++= commonDependencies ++ Seq (
      "com.fasterxml.jackson.dataformat" % "jackson-dataformat-xml" % "2.19.1",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.19.1",
      "org.scala-lang.modules" %% "scala-xml" % "2.4.0",
    ).map(_ withSources() withJavadoc())
  )

lazy val commonDependencies = Seq(
  "com.augustnagro" %% "magnum" % "1.3.1",
  "com.mysql" % "mysql-connector-j" % "9.3.0",
  "org.apache.commons" % "commons-text" % "1.13.1"
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


libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test

ThisProject / scalacOptions ++= Seq(
  "-groups",
  "-Xwiki-syntax",
  "-experimental"
)





