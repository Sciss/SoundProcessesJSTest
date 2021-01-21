lazy val deps = new {
  val main = new {
    val audioFile       = "2.3.2"
    val fscape          = "3.6.0-SNAPSHOT"
    val laminar         = "0.11.0"
    val lucre           = "4.4.0"
    val lucreSwing      = "2.6.0"
    val plotly          = "0.8.0"
    val scalaJavaTime   = "2.1.0"
    val soundProcesses  = "4.7.0-SNAPSHOT"
  }
}

lazy val root = project.in(file("."))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "SoundProcesses JS Test",
    scalaVersion := "2.13.4",
    // This is an application with a main method
    scalaJSUseMainModuleInitializer := true,
//    resolvers += Resolver.bintrayRepo("cibotech", "public"),  // needed for EvilPlot
    libraryDependencies ++= Seq(
      "com.raquo" %%% "laminar"               % deps.main.laminar,
      "de.sciss"  %%% "audiofile"             % deps.main.audioFile,
      "de.sciss"  %%% "fscape-lucre"          % deps.main.fscape,
      "de.sciss"  %%% "lucre-core"            % deps.main.lucre,
      "de.sciss"  %%% "lucre-expr"            % deps.main.lucre,
      "de.sciss"  %%% "lucre-swing"           % deps.main.lucreSwing,
      "de.sciss"  %%% "soundprocesses-core"   % deps.main.soundProcesses,
      "de.sciss"  %%% "soundprocesses-views"  % deps.main.soundProcesses,
      "org.plotly-scala" %%% "plotly-render" % deps.main.plotly,
      "io.github.cquiroz" %%% "scala-java-time" % deps.main.scalaJavaTime,
    ),
    artifactPath in(Compile, fastOptJS) := baseDirectory.value / "lib" / "main.js",
    artifactPath in(Compile, fullOptJS) := baseDirectory.value / "lib" / "main.js",
  )

