ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.6.4"
enablePlugins(ShadingPlugin)

lazy val root = (project in file("."))
  .settings(
    name := "ITSAHardwareLogger"
  )



// sbt
libraryDependencies += "com.softwaremill.macwire" %% "macros" % "2.6.6" % "provided" withSources() withJavadoc()
libraryDependencies ++= {
  // Determine an OS version of JavaFX binaries
  lazy val osName = System.getProperty("os.name") match {
    case n if n.startsWith("Linux") => "linux"
    case n if n.startsWith("Mac") => "mac"
    case n if n.startsWith("Windows") => "win"
    case _ => throw new Exception("Unknown platform!")
  }
  Seq("base", "controls", "fxml", "graphics", "media", "swing", "web")
    .map(m => "org.openjfx" % s"javafx-$m" % "23" classifier osName).map(_.withJavadoc() withSources())
}



libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test

ThisProject / scalacOptions ++= Seq(
  "-groups",
  "-Xwiki-syntax"
)