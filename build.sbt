lazy val deps = new {
  val main = new {
    val audioFile       = "2.3.1"
    val fscape          = "3.2.0"
    val laminar         = "0.11.0"
    val lucre           = "4.2.0"
    val lucreSwing      = "2.3.0"
    val plotly          = "0.8.0"
    val soundProcesses  = "4.3.0"
  }
}

lazy val root = project.in(file("."))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "SoundProcesses JS Test",
    scalaVersion := "2.13.3", // or any other Scala version >= 2.11.12
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
    )
  )

