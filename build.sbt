import sbt.Keys.libraryDependencies


ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.6.4"
ThisBuild / assemblyMergeStrategy := {
  case PathList("META-INF", _*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}

lazy val javaFXDeps = Seq("win", "mac", "linux").flatMap { osName =>
  Seq("base", "controls")
    .map(m => "org.openjfx" % s"javafx-$m" % "23" classifier "win")
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
    name := "ITSAHardwareLogger",

    libraryDependencies ++= uiDependencies,
    libraryDependencies += "com.github.oshi" % "oshi-core" % "6.8.0",
    libraryDependencies ++= commonDependencies,
    scalacOptions += "-experimental"

  ).dependsOn(common, backend, hdsentinelreader)


lazy val common: Project = (project in file("Common"))
  .settings(
    name := "Common",
    libraryDependencies ++= commonDependencies

  )

lazy val commonDependencies = Seq(
  "com.augustnagro" %% "magnum" % "1.3.1",
  "com.mysql" % "mysql-connector-j" % "9.2.0",
  "com.softwaremill.ox" %% "core" % "0.5.13",
).map(_ withJavadoc() withSources())

lazy val hdsentinelreader = (project in file("HDSentinelReader")).
  settings(
    name := "HDSentinelReader",

      libraryDependencies ++= Seq(
      "com.fasterxml.jackson.dataformat" % "jackson-dataformat-xml" % "2.18.3",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.18.3",
        "org.scala-lang.modules" %% "scala-xml" % "2.3.0",
      ).map(_ withJavadoc() withSources()),
    scalacOptions += "-experimental"
  ).dependsOn(common)

lazy val backend = (project in file("Backend"))
  .settings(
    name := "Backend",
    libraryDependencies ++= commonDependencies,
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test,
    unmanagedSourceDirectories in Test += file("tests")
  ).dependsOn(common)



//libraryDependencies ++= {
//  // Determine an OS version of JavaFX binaries
//  lazy val osName = System.getProperty("os.name") match {
//    case n if n.startsWith("Linux") => "linux"
//    case n if n.startsWith("Mac") => "mac"
//    case n if n.startsWith("Windows") => "win"
//    case _ => throw new Exception("Unknown platform!")
//  }
//  lazy val classifiers =Seq("win", "linux", "mac")
//    classifiers.map { osName =>
//
//        Seq("base", "controls", "fxml", "graphics", "media", "swing", "web")
//          .map(m => "org.openjfx" % s"javafx-$m" % "23" classifier osName).map(_.withJavadoc() withSources())
//    }
//}



libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test

ThisProject / scalacOptions ++= Seq(
  "-groups",
  "-Xwiki-syntax",
  "-experimental"
)



