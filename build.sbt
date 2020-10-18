lazy val deps = new {
  val main = new {
    val lucre = "4.1.0-SNAPSHOT"
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
      "de.sciss" %%% "lucre-core" % deps.main.lucre,
      "de.sciss" %%% "lucre-expr" % deps.main.lucre,
    ),
  )

