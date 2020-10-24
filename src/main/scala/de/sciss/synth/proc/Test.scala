package de.sciss.synth.proc

import com.raquo.laminar.api.L.{render, documentEvents, unsafeWindowOwner}
import de.sciss.fscape
import de.sciss.fscape.GE
import de.sciss.fscape.lucre.FScape
import de.sciss.lucre.edit.UndoManager
import de.sciss.lucre.expr
import de.sciss.lucre.swing
import de.sciss.lucre.synth.InMemory
import de.sciss.synth.proc
import org.scalajs.dom

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("Test")
object Test {
  def main(args: Array[String]): Unit = {
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

  @JSExport
  def run(): Unit = {
    type S = InMemory
    type T = InMemory.Txn

    SoundProcesses.init()
    FScape.init()

//    val cfg = FScape.Config()
//    cfg.blockSize = 4096
//    FScape.defaultConfig = cfg

    val gFScRMS = fscape.Graph {
      import fscape.graph._
      import fscape.lucre.graph._

      val SR  = 44100
      val m   = 100 * SR
      val n   = WhiteNoise().take(m)
      val f   = LPF(n, 200.0/SR)
      val rms = (RunningSum(f.squared).last / m).sqrt
      MkDouble("out", rms)
    }

    lazy val gFSc1 = fscape.Graph {
      import fscape.graph._
      import fscape.lucre.graph._

      val n       = WhiteNoise()
      val SR      = 48000
      val modFreq = Seq[GE](0.1, 0.123).map(_ / SR)
      val freq    = SinOsc(modFreq).linExp(-1, 1, 200, 2000)
      val f       = LPF(n, freq / SR)
//      val nw    = 8192 * 4
//      val norm  = NormalizeWindow(f, nw) * 0.25
//      val normW = norm * GenWindow.Hann(nw)
//      val lap   = OverlapAdd(normW, nw, nw / 2)
//      val sig   = lap
      val sig   = f * 100
      Frames(sig.out(0)).poll(Metro(SR), "metro")
      WebAudioOut(sig)
    }

    def any2stringadd: Any = ()

    lazy val gFScBubbles = fscape.Graph {
      import fscape.graph._

      val SR    = 44100
      val f     = (LFSaw(0.4 / SR) * 24 + (LFSaw(Seq[GE](8.0/SR, 7.23/SR)) * 3 + 80)).midiCps // glissando function
      val fl    = f // OnePole(f, 0.995)
      val sin   = SinOsc(fl / SR) * 0.04
      val echoL = 0.2 * SR
      val echo  = CombN(sin, echoL, echoL, 4 * SR) // echoing sine wave
      val sig   = echo
//      Frames(sig.out(0)).poll(Metro(SR), "metro")
      WebAudioOut(sig)
    }

    lazy val gEx = expr.Graph {
      import expr.graph._
      import expr.ExImport._

      val fsc = Runner("fsc")
      val res = Var(0.0)

      LoadBang() ---> Act(
        PrintLn("Hello from SoundProcesses. Running FScape..."),
        Delay(3.0)(PrintLn("3 seconds have passed. java.vm.name = " ++ Sys.Property("java.vm.name").getOrElse("not defined"))),
        fsc.runWith("out" -> res)
      )

      fsc.done ---> Act(
        PrintLn(Const("FScape completed. RMS is %1.1f dBFS.").format(res.ampDb))
      )
    }

    lazy val gW0 = swing.Graph {
      import expr.graph._
      import expr.ExImport._
      import swing.graph._

      val rRMS      = Runner("fsc-rms")
      val rBubbles  = Runner("fsc-bubbles")
      val rms       = Var(0.0)
      val state     = Var("")
      val rmsInfo   = Const("RMS is %1.1f dBFS.").format(rms.ampDb)

      val ggAnalyze = Button("Analyze")
      ggAnalyze.clicked ---> Act(
        state.set("..."),
        rRMS.runWith("out" -> rms),
      )
      ggAnalyze.enabled = {
        val s = rRMS.state
        ((s sig_== 0) || (s >= 4))
      }

      val ggStartBubbles = Button("Play")
      val ggStopBubbles  = Button("Stop")
      ggStartBubbles.clicked ---> rBubbles.run
      ggStopBubbles .clicked ---> rBubbles.stop
      ggStartBubbles.enabled = {
        val s = rBubbles.state
        ((s sig_== 0) || (s >= 4))
      }
      ggStopBubbles.enabled = !ggStartBubbles.enabled

      LoadBang() ---> Act(
        PrintLn("Hello from SoundProcesses. Running FScape..."),
        Delay(3.0)(PrintLn("3 seconds have passed. java.vm.name = " ++ Sys.Property("java.vm.name").getOrElse("not defined"))),
//        fsc.runWith("out" -> res)
      )

      rRMS.done ---> Act(
        PrintLn("FScape completed."),
        state.set(rmsInfo),
      )

      val pBubbles = FlowPanel(Label("Analog Bubbles:"), ggStartBubbles, ggStopBubbles)

      val pRMS = FlowPanel(Label("Filtered Noise:"), ggAnalyze, Label(state))
//      pRMS.hGap = 10

      val ggCheck = CheckBox("Checkbox:")
      val checkState = ggCheck.selected()
      checkState.changed ---> PrintLn("SELECTED = " ++ checkState.toStr ++ " / " ++ ggCheck.selected().toStr)

      BorderPanel(
        north   = pBubbles,
        center  = ggCheck,
        south   = pRMS,
      )
    }

    lazy val gW1 = swing.Graph {
      import swing.graph._

      BorderPanel(
        north = Label("north"),
        south = Label("south"),
        center= Label("center"),
        west  = Label("west"),
        east  = Label("east"),
      )
    }

    val gW = gW0

    implicit val system: S = InMemory()
    implicit val undo: UndoManager[T] = UndoManager()

//    import Workspace.Implicits._

    val view = system.step { implicit tx =>
      implicit val u: Universe[T] = Universe.dummy[T]

      val fscRMS = FScape[T]()
      fscRMS.graph() = gFScRMS
      val fscBubbles = FScape[T]()
      fscBubbles.graph() = gFScBubbles

//      val w = proc.Control[T]()
      val w = Widget[T]()
      w.graph() = gW
      w.attr.put("fsc-rms"    , fscRMS    )
      w.attr.put("fsc-bubbles", fscBubbles)

//      val r = proc.Runner(w)
////      println(r)
//      r.run()
//      r.reactNow { implicit tx => state =>
//        println(s"STATE = $state")
//      }

      val wH = tx.newHandle(w)
      implicit val ctx: expr.Context[T] = ExprContext(selfH = Some(wH))

      val _view = gW.expand[T]
      _view.initControl()
      _view
//
//
//      gEx.expand.initControl()
    }

    val appContainer: dom.Element = dom.document.body // .querySelector("#appContainer")
    /*val root: RootNode =*/ render(appContainer, view.component)

    println("End of main.")
  }
}