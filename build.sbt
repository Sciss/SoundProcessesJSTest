lazy val deps = new {
  val main = new {
    val fscape          = "3.1.0"
    val lucre           = "4.1.0"
    val lucreSwing      = "2.2.0"
    val soundProcesses  = "4.2.0"
  }
}

lazy val root = project.in(file("."))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "SoundProcesses JS Test",
    scalaVersion := "2.13.3", // or any other Scala version >= 2.11.12
    // This is an application with a main method
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "de.sciss" %%% "fscape-lucre"         % deps.main.fscape,
      "de.sciss" %%% "lucre-core"           % deps.main.lucre,
      "de.sciss" %%% "lucre-expr"           % deps.main.lucre,
      "de.sciss" %%% "lucre-swing"          % deps.main.lucreSwing,
      "de.sciss" %%% "soundprocesses-core"  % deps.main.soundProcesses,
      "de.sciss" %%% "soundprocesses-views" % deps.main.soundProcesses,
    ),
  )

