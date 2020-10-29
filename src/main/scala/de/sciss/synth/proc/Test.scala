package de.sciss.synth.proc

import com.raquo.laminar.api.L.{documentEvents, render, unsafeWindowOwner}
import de.sciss.fscape
import de.sciss.fscape.GE
import de.sciss.fscape.lucre.FScape
import de.sciss.lucre.edit.UndoManager
import de.sciss.lucre.{expr, swing}
import de.sciss.lucre.synth.InMemory
import org.scalajs.dom

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("Test")
object Test {
  def main(args: Array[String]): Unit = {
    IndexedDBTests.run()
  }

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
      val run = RunningSum(f.squared)
      ProgressFrames(run, m)
      val rms = (run.last / m).sqrt
      MkDouble("out", rms)
    }

    lazy val gFSc1 = fscape.Graph {
      import fscape.graph._

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
      import de.sciss.fscape.lucre.graph.Ops._
      import fscape.graph._

      val SR      = 44100
      val fmOff   = "fm-offset" .attr(80)
      val fmODepth= "fm-depth"  .attr(24)
      val hasVerb = "reverb"    .attr(1)
      val lfFreq  = "lf-freq"   .attr(0.4)
      // glissando function
      val f       = (LFSaw(lfFreq / SR) * fmODepth + (LFSaw(Seq[GE](8.0/SR, 7.23/SR)) * 3 + fmOff)).midiCps
      val fl      = f // OnePole(f, 0.995)
      val sin     = SinOsc(fl / SR) * 0.04
      val sig     = If (hasVerb) Then {
        val echoL = 0.2 * SR
        CombN(sin, echoL, echoL, 4 * SR) // echoing sine wave
      } Else sin
//      Frames(sig.out(0)).poll(Metro(SR), "metro")
      WebAudioOut(sig)
    }

    lazy val gEx = expr.Graph {
      import expr.ExImport._
      import expr.graph._

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
      import expr.ExImport._
      import expr.graph._
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

      val slFMOff = Slider()
      slFMOff.min     =  40
      slFMOff.max     = 120
      slFMOff.value() =  80

      val ifFMDepth = IntField()
      ifFMDepth.min     =   0
      ifFMDepth.max     =  96
      ifFMDepth.value() =  24
      ifFMDepth.unit    = "semitones"

      val dfLFFreq = DoubleField()
      dfLFFreq.min     =    0.01
      dfLFFreq.max     =  100.0
      dfLFFreq.value() =    0.4
      dfLFFreq.unit    = "Hz"

      val cbReverb = CheckBox("Reverb")
      cbReverb.selected() = true

      val ggComboBox = ComboBox(
        Seq("One", "Two", "Three")
      )
      ggComboBox.index() = 1

      {
        val idx = ggComboBox.index()
        idx.changed ---> PrintLn("ComboBox index = " ++ idx.toStr)
      }

      val bang = Bang()

      val ggStartBubbles = Button("Play")
      val ggStopBubbles  = Button("Stop")
      ggStartBubbles.clicked ---> rBubbles.runWith(
        "fm-offset" -> slFMOff  .value(),
        "fm-depth"  -> ifFMDepth.value(),
        "lf-freq"   -> dfLFFreq .value(),
        "reverb"    -> cbReverb .selected(),
      )
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
        bang,
      )

//      val pBubbles = FlowPanel(
//        Label("Analog Bubbles:"),
//        ggStartBubbles, ggStopBubbles,
//        Label(" Freq Mod Offset:"), slFMOff,
//        Label(" Freq Mod Depth:"), ifFMDepth,
//        Label(" LFO Freq:"), dfLFFreq,
//      )

      val pBubbles = GridPanel(
        Label("Analog Bubbles:"), FlowPanel(ggStartBubbles, ggStopBubbles),
        Label(" Freq Mod Offset:"), slFMOff,
        Label(" Freq Mod Depth:"), ifFMDepth,
        Label(" LFO Freq:"), dfLFFreq,
      )
      pBubbles.columns        = 2
      pBubbles.compactColumns = true

      val progBar = ProgressBar()
      val prog    = (rRMS.progress * 100).toInt
      progBar.value = prog

//      prog.changed ---> PrintLn("PROGRESS = " ++ prog.toStr)

//      val pRMS = FlowPanel(Label("Filtered Noise:"), ggAnalyze, progBar, bang, Label(state))

      val pRMS = GridPanel(
        Label("Filtered Noise:"), FlowPanel(ggAnalyze, progBar),
        Empty(), FlowPanel(bang, Label(state)),
      )
      pRMS.columns        = 2
      pRMS.compactColumns = true

//      pRMS.hGap = 10

//      val checkState = cbReverb.selected()
//      checkState.changed ---> PrintLn("SELECTED = " ++ checkState.toStr ++ " / " ++ cbReverb.selected().toStr)

      BorderPanel(
        north   = pBubbles,
        center  = FlowPanel(cbReverb, ggComboBox),
        south   = pRMS,
      )
    }

    lazy val gW1 = swing.Graph {
      import swing.graph._

      BorderPanel(
        north = Label("north"),
        south = Label("south"),
//        center= Separator(),
        center= Label("center"),
        west  = Label("west"),
        east  = Label("east"),
      )
    }

    lazy val gW2 = swing.Graph {
      import expr.graph._
      import swing.graph._

      val gp1 = GridPanel(
        Button("One"),
        Button("Two"),
        Button("Three"),
        Button("Four hundred"),
      )
      gp1.columns = 2

      val ggText = TextField()
      val vText = ggText.text()
      val lbText = Label(vText)
      vText.changed ---> PrintLn(Const("NEW TEXT ") ++ vText)

      val gp2 = GridPanel(
        Button("One"),
        Button("Two"),
        ggText,
        lbText,
      )
      gp2.columns = 2
      gp2.compactColumns = true

      BorderPanel(
        north = gp1, south = gp2
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