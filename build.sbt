import sbt.Keys.libraryDependencies


ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.6.4"
ThisBuild / assemblyMergeStrategy := {
  case PathList("META-INF", _*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}

lazy val uiDependencies = Seq("win", "mac", "linux").flatMap { osName =>
  Seq("base", "controls", "fxml", "graphics", "media", "swing", "web")
    .map(m => "org.openjfx" % s"javafx-$m" % "23" classifier "win").map(_.withJavadoc() withSources())
} :+ ("org.scalafx" %% "scalafx" % "23.0.1-R34" withJavadoc() withSources())


lazy val root = (project in file("."))
  .settings(
    assembly / assemblyJarName := "ITSAHardwareLogger.jar",
    name := "ITSAHardwareLogger",

    libraryDependencies ++= uiDependencies,
    libraryDependencies += "com.github.oshi" % "oshi-core" % "6.8.0",
    libraryDependencies ++= commonDependencies

  ).dependsOn(common, backend, hdsentinelreader)


lazy val common = (project in file("Common"))
  .settings(
    name := "Common",
    libraryDependencies ++= commonDependencies

  )


lazy val commonDependencies = Seq(
  "com.augustnagro" %% "magnum" % "1.3.1",
  "com.mysql" % "mysql-connector-j" % "9.2.0",
  "com.softwaremill.ox" %% "core" % "0.5.13",
  "co.blocke" %% "scala-reflection" % "2.0.11"
).map(_ withJavadoc() withSources())

//lazy val macros = (project in file("Macros"))
//  .settings(
//    name := "Macros",
//    libraryDependencies ++= uiDependencies
//  ).dependsOn(common)

//lazy val hdsentinel = (project in file("HDSentinelReader"))
//  .enablePlugins(ScalaxbPlugin)
//  .settings(
//    name := "HDSentinelReader",
//    libraryDependencies ++= Seq(
//      "org.scalaxb" %% "scalaxb" % "1.12.2", //withSources() withJavadoc(),
//      "org.scala-lang.modules" %% "scala-xml" % "2.3.0",
//
//    )
//
//  )

//import sbtscalaxb.ScalaxbKeys.*
//
//lazy val OIH = config("org.itsadigitaltrust.hdsentinelreader").extend(Compile)
//

lazy val dispatchVersion = "2.0.0"
lazy val dispatch = "org.dispatchhttp" %% "dispatch-core" % dispatchVersion
lazy val jaxbApi = "javax.xml.bind" % "jaxb-api" % "2.3.1"
lazy val scalaXml = "org.scala-lang.modules" %% "scala-xml" % "2.3.0"
lazy val scalaParser = "org.scala-lang.modules" %% "scala-parser-combinators" % "2.4.0"

lazy val hdsentinelreader = (project in file("HDSentinelReader")).
  enablePlugins(ScalaxbPlugin).
  settings(
    name := "HDSentinelReader",
    // Compile / scalaxb / scalaxbAutoPackages := true,
    // https://mvnrepository.com/artifact/com.fasterxml.jackson.module/jackson-module-scala
    libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.18.3",
      libraryDependencies += "com.fasterxml.jackson.dataformat" % "jackson-dataformat-xml" % "2.18.3",
  )

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



