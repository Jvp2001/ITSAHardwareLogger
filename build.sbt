import sbt.Keys.libraryDependencies

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.6.4"
ThisBuild / assemblyMergeStrategy :=
  {
    case PathList("META-INF", _*) => MergeStrategy.discard
    case _ => MergeStrategy.first
  }

lazy val root = (project in file("."))
  .settings(
    assembly / assemblyJarName := "ITSAHardwareLogger.jar",
    name := "ITSAHardwareLogger",

    libraryDependencies += "org.scalafx" %% "scalafx" % "23.0.1-R34",
    libraryDependencies ++= Seq("win", "mac", "linux").flatMap
    { osName =>
      Seq("base", "controls", "fxml", "graphics", "media", "swing", "web")
        .map(m => "org.openjfx" % s"javafx-$m" % "23" classifier "win").map(_.withJavadoc() withSources())
    },

    libraryDependencies += "com.github.oshi" % "oshi-core" % "6.7.1",
    libraryDependencies ++= commonDependencies

  ).dependsOn(common, macros)


lazy val common = (project in file("Common"))
  .settings(
    name := "Common",
    libraryDependencies ++= commonDependencies

  )


lazy val commonDependencies = Seq(
  "com.augustnagro" %% "magnum" % "1.3.1" withJavadoc() withSources(),
  "com.mysql" % "mysql-connector-j" % "9.2.0" withSources() withJavadoc()
)

lazy val uiDependencies = Seq("org.scalafx" %% "scalafx" % "23.0.1-R34") ++:
  Seq("win", "mac", "linux").flatMap
  { osName =>
    Seq("base", "controls", "fxml", "graphics", "media", "swing", "web")
      .map(m => "org.openjfx" % s"javafx-$m" % "23" classifier "win").map(_.withJavadoc() withSources())
  }
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



