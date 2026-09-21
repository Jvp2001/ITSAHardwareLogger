
ThisBuild / packageOptions += Package.ManifestAttributes(
  "Build-Time" -> java.time.Instant.now.toString
)
ThisBuild / normalizedName := "ITSA Hardware Logger"
ThisBuild / version := "0.2.6-SNAPSHOT"
ThisBuild / organization := "org.itsadigitaltrust"
ThisBuild / scalaVersion := "3.9.0"
ThisBuild / organizationName := "ITSA Digital Trust"
ThisBuild / scalaBinaryVersion := "3"
ThisBuild / organizationHomepage := Some(url("https://itsadigitaltrust.org/"))
ThisBuild / homepage := Some(url("https://github.com/jvp2001/ITSAHardwareLogger"))
ThisBuild / licenses += "GNUv3" -> url("https://www.gnu.org/licenses/gpl-3.0.en.html#license-text")
ThisBuild / scalacOptions ++= Seq("-java-output-version", "25")
// Optional: Enforce JDK 24 for Java sources if mixed
ThisBuild / javacOptions ++= Seq("--release", "25")

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
  "-experimental",
  "-explain-cyclic",
  "-preview",
  "--Yrelaxed-lambdas",
  "--Ydebug-cyclic",
  "--explain-cyclic",
  "-Xno-enrich-error-messages"
)


Global / onLoad := {
  val previousOnLoad = (Global / onLoad).value
  previousOnLoad.andThen { state =>
    // Inject your command or task sequence here
    "buildInfo" :: state
  }
}

// Forces buildInfo to execute whenever the compile task is triggered
(Compile / compile) := ((Compile / compile) dependsOn (Compile / buildInfo)).value

// Forces compile to execute whenever the assembly task is triggered
assembly := (assembly dependsOn (Compile / compile)).value

fork := true

val runtimeJavaOptions = Seq(
  "-Djavafx.preloader=SplashPreloader",
  "--enable-native-access=javafx.graphics"
)
javaOptions ++= runtimeJavaOptions

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
    xs map {
      _.toLowerCase
    } match {
      case "services" :: xs =>
        MergeStrategy.filterDistinctLines

      case _ => MergeStrategy.discard
    }
  case _ => MergeStrategy.first
}


// for the log viewer based dependencies
resolvers += "SciJava Public" at "https://maven.scijava.org/content/repositories/public/"


val javafxVersion = "25.0.3"
val platforms = Seq("linux")
val modules = Seq("base", "controls", "web")

lazy val javaFXDeps = platforms
  .flatMap(platform =>
    modules
      .map(module => "org.openjfx" % s"javafx-$module" % javafxVersion classifier platform)
  )

lazy val scalaFXDeps = Seq(
  "org.scalafx" %% "scalafx" % "26.0.0-R38",
  "org.scalafx" %% "scalafx-extras" % "0.12.1",
)

lazy val atlantaFX = Seq(
  "io.github.mkpaz" % "atlantafx-base" % "2.1.0"
)
lazy val logViewer = Seq(
  // Source: https://mvnrepository.com/artifact/io.github.qupath/logviewer-ui-main
  "io.github.qupath" % "logviewer-ui-main" % "0.3.0",
  "io.github.qupath" % "logviewer-logging-logback" % "0.2.0",
)


lazy val uiDependencies = (javaFXDeps ++ scalaFXDeps ++ atlantaFX ++ logViewer)
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
    assembly / assemblyOutputPath := target.value / s"scala-${scalaVersion.value}" / s"${(ThisBuild / assemblyTargetOs).value}/ITSAHardwareLogger.jar",
    name := "ITSAHardwareLogger",


    libraryDependencies ++= uiDependencies,
    libraryDependencies += "com.github.oshi" % "oshi-core" % "7.6.1",
    //    libraryDependencies ++= commonDependencies,

    // addForegroundTask scalatest
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.20" % Test,
    scalacOptions += "-experimental"

  ).dependsOn(common, backend, hdsentinelreader, issueReporter)


lazy val common = (project in file("Common"))
  .settings(
    name := "Common",
    scalaVersion := "3.9.0",
    libraryDependencies ++= commonDependencies
  ).dependsOn()
lazy val loggingDeps = Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.6",
  "ch.qos.logback" % "logback-classic" % "1.6.3"
)

lazy val commonDependencies = Seq(
  "org.apache.commons" % "commons-text" % "1.15.0",
  //  "org.checkerframework" % "checker" % "3.54.0",
  "com.softwaremill.ox" %% "core" % "1.0.7",
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-xml" % "2.22.2",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.22.2",
  "org.scala-lang.modules" %% "scala-xml" % "2.5.0",
  "com.somainer" %% "scala3-nameof" % "0.0.1",
  "com.github.pureconfig" %% "pureconfig-core" % "0.17.8"

) ++ loggingDeps.map(_ withJavadoc() withSources())

lazy val hdsentinelreader = (project in file("HDSentinelReader")).
  settings(
    name := "HDSentinelReader",

    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.dataformat" % "jackson-dataformat-xml" % "2.22.2",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.22.2",
      "org.scala-lang.modules" %% "scala-xml" % "2.5.0",
    ).map(_ withJavadoc() withSources()),
    scalacOptions += "-experimental"
  ).dependsOn(common)

lazy val backend = (project in file("Backend"))
  .settings(
    name := "Backend",
    libraryDependencies ++= commonDependencies,
    libraryDependencies ++= Seq(
      "com.augustnagro" %% "magnum" % "1.3.1",
      "com.mysql" % "mysql-connector-j" % "26.7.0",
    ).map(_ withSources() withJavadoc()),
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
commands += Command.single("assemblyFor") { (state, os) =>
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
  Command.command("assembleForWindows")(state => "assemblyFor win" :: state),
  Command.command("assembleForMac")(state => "assemblyFor mac" :: state),
  Command.command("assembleForLinux")(state => "assemblyFor linux" :: state)
)