package de.sciss.proc

import com.raquo.laminar.api.L.{documentEvents, render, unsafeWindowOwner}
import de.sciss.asyncfile.AsyncFile
import de.sciss.audiofile.AudioFile
import de.sciss.log.Level
import de.sciss.lucre.swing.LucreSwing
import de.sciss.lucre.synth.Executor
import de.sciss.synth.{Server => SServer}
import de.sciss.{fscape, osc, synth}
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import scala.util.{Failure, Success}

@JSExportTopLevel("Test")
object Test {
  def main(args: Array[String]): Unit = {
    //    IndexedDBTests.run()
    runGUI()
    //    PlotlyTest.run()
  }

  //    type S = InMemory
  //    type T = InMemory.Txn
  type S = Durable
  type T = Durable.Txn

  def runGUI(): Unit = {
    println("Test initialized.")
    documentEvents.onDomContentLoaded.foreach { _ =>
      run()
    } (unsafeWindowOwner)
  }

  def run2(): Unit = {
    import com.raquo.laminar.api.L._
    val appContainer: dom.Element = dom.document.body
    val inp =
      input(
        tpe := "checkbox"
      )

    def mkObs(name: String) = Observer[dom.MouseEvent](_ => println(s"$name sees " + inp.ref.checked))

    inp.amend(
      onClick --> mkObs("observer-1")
    )
    inp.amend(
      onClick --> mkObs("observer-2")
    )

    val c = label(
      "example",
      inp
    )
    render(appContainer, c)
  }

  private var universeOpt = Option.empty[Universe[T]]

  @JSExport
  def startAural(): Unit = {
    universeOpt.fold[Unit] {
      println("SoundProcesses is not initialized yet.")
    } { u =>
      u.cursor.step { implicit tx =>
        val sCfg = synth.Server.Config()
        val cCfg = synth.Client.Config()
        sCfg.inputBusChannels   = 0
        sCfg.outputBusChannels  = 2
        sCfg.transport          = osc.Browser
//        u.auralSystem.start(sCfg, cCfg, connect = true)
        u.auralSystem.connect(sCfg, cCfg)
      }
    }
  }

  @JSExportTopLevel("dumpOSC")
  def dumpOSC(code: Int = 1): Unit =
    SServer.default.dumpOSC(osc.Dump(code))

  @JSExportTopLevel("dumpTree")
  def dumpTree(): Unit = {
    import de.sciss.synth.Ops._
    SServer.default.dumpTree(controls = true)
  }

  @JSExportTopLevel("cmdPeriod")
  def cmdPeriod(): Unit = {
    import de.sciss.synth.Ops._
    SServer.default.freeAll()
  }

  @JSExportTopLevel("serverCounts")
  def serverCounts(): Unit =
    println(SServer.default.counts)

  @JSExportTopLevel("sendOSC")
  def sendOSC(cmd: String, args: js.Any*): Unit =
    SServer.default.!(osc.Message(cmd, args: _*))

  @JSExport
  def run(): Unit = {

    FScape        .init()
    LucreSwing    .init()
    SoundProcesses.init()
    Widget        .init()

    AsyncFile.log.level       = Level.Info  // Debug
    AudioFile.log.level       = Level.Info  // Debug
    fscape.Log.stream.level   = Level.Off   // Level.Info // Debug
    fscape.Log.control.level  = Level.Off   // Level.Info // Debug

    AsyncFile.log.out         = Console.out
    AudioFile.log.out         = Console.out
    fscape.Log.stream.out     = Console.out
    fscape.Log.control.out    = Console.out

    val appContainer: dom.Element = dom.document.body // .querySelector("#appContainer")

//    val fut = DirectWorkspace()
    val fut = LoadWorkspace()

    fut.onComplete {
      case Success((universe, view)) =>
        universeOpt = Some(universe)
        /*val root: RootNode =*/ render(appContainer, view.component)
      case Failure(ex) =>
        ex.printStackTrace()
    } (Executor.executionContext)

    println("End of main.")
  }
}
