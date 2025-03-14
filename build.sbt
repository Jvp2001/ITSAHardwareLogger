import sbt.Keys.libraryDependencies

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.6.4"
ThisBuild / assemblyMergeStrategy := {
  case PathList("META-INF", _*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}

lazy val root = (project in file("."))
  .settings(
    assembly / assemblyJarName := "ITSAHardwareLogger.jar",
    name := "ITSAHardwareLogger",
        
    libraryDependencies ++= Seq("win", "mac", "linux").flatMap { osName =>
      Seq("base", "controls", "fxml", "graphics", "media", "swing", "web")
        .map(m => "org.openjfx" % s"javafx-$m" % "23" classifier "win").map(_.withJavadoc() withSources())
    },

    libraryDependencies += "com.github.oshi" % "oshi-core" % "6.7.0",

).dependsOn(common, macros)


lazy val common = (project in file("Common"))
  .settings(
    name := "Common",
    libraryDependencies += "org.springframework.boot" % "spring-boot-starter" % "3.4.3" withSources() withJavadoc(),

  )


lazy val macros = (project in file("Macros"))
  .settings(
    name := "Macros",
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



