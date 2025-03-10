
ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.6.4"
ThisBuild / assemblyMergeStrategy := {
  case PathList("META-INF", _*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}
//ThisBuild / assemblyShadeRules := Seq(
//  ShadeRule.rename(
////    "javafx.*" -> "org.itsadigitaltrust.deps.javafx",
//    "oshi.**" -> "org.itsadigitaltrust.deps.oshi",
//  ).inAll
//)
lazy val root = (project in file("."))
  .settings(
    assembly / assemblyJarName := "ITSAHardwareLogger.jar",
    name := "ITSAHardwareLogger"

  )



libraryDependencies += "com.softwaremill.macwire" %% "macros" % "2.6.6" % "provided" withSources() withJavadoc()
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



 libraryDependencies ++= Seq("win", "mac", "linux").flatMap { osName =>
     Seq("base", "controls", "fxml", "graphics", "media", "swing", "web")
       .map(m => "org.openjfx" % s"javafx-$m" % "23" classifier "win").map(_.withJavadoc() withSources())
 }
libraryDependencies += "com.github.oshi" % "oshi-core" % "6.7.0"


libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test

ThisProject / scalacOptions ++= Seq(
  "-groups",
  "-Xwiki-syntax"
)



