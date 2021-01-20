package de.sciss.proc

import de.sciss.fscape.GE
import de.sciss.lucre.edit.UndoManager
import de.sciss.lucre.store.InMemoryDB
import de.sciss.lucre.swing.View
import de.sciss.lucre.synth.{RT, Server}
import de.sciss.lucre.{expr, swing, Artifact => LArtifact, ArtifactLocation => LArtifactLocation}
import de.sciss.proc.Test.{S, T}
import de.sciss.synth.{SynthGraph, ugen}
import de.sciss.{fscape, proc, synth}

import java.net.URI
import scala.concurrent.Future

// No longer used. We load a binary workspace blob now.
object DirectWorkspace {
  def apply(): Future[(Universe[T], View[T])] = {
    implicit val cursor: S = Durable(InMemoryDB())

    //    val cfg = FScape.Config()
    //    cfg.blockSize = 4096
    //    FScape.defaultConfig = cfg

    val gFScRMS_OLD = fscape.Graph {
      import fscape.Ops._
      import fscape.graph._
      import fscape.lucre.graph._

      val SR = 44100
      val m = 12 /*100*/ * SR
      val n = WhiteNoise().take(m)
      val lf = Line(0.2, 2.0, m)
      val fo = Line(600, 1200, m)
      val freq = SinOsc(lf / SR).linExp(-1, 1, 300, fo)
      val f = LPF(n, freq / SR) * 100
      val sum = RunningSum(f.squared)
      ProgressFrames(sum, m)
      val rms = (sum.last / m).sqrt
      //      val f = n
      /*val num =*/ AudioFileOut("file", f, sampleRate = SR)
      //      Length(num).poll("POLLED")
      MkDouble("out", rms)
    }

    val gFScRMS = fscape.Graph {
      import fscape.graph._
      import fscape.lucre.graph.Ops._
      import fscape.lucre.graph._

      val SR = 44100 // XXX TODO: for real-time input, should use correct value
      val m = 12 /*100*/ * SR
      val srcIdx = "source".attr(0)
      val source = If(srcIdx sig_== 0) Then WhiteNoise() Else PhysicalIn()
      val n = source.take(m)
      val lf = Line(0.2, 2.0, m)
      val fo = Line(600, 1200, m)
      val freq = SinOsc(lf / SR).linExp(-1, 1, 300, fo)
      val f = LPF(n, freq / SR) * 100

      //      val f = {
      ////        val kernelLen = 256
      ////        val kernel    = LFSaw(440/SR).take(kernelLen) * 0.05
      ////        Convolution(n, kernel, kernelLen)
      //        val fwd = Real1FFT(n, 512, mode = 0)
      //        val flt = fwd // * RepeatWindow(fwd.complex.mag > -40.dbAmp)
      //        Real1IFFT(flt, 512, mode = 0)
      //      }

      val sum = RunningSum(f.squared)
      ProgressFrames(sum, m)
      val rms = (sum.last / m).sqrt
      //      val f = n
      /*val num =*/ AudioFileOut("file", f, sampleRate = SR)
      //      Length(num).poll("POLLED")
      MkDouble("out", rms)
    }

    val gFScReplay = fscape.Graph {
      import fscape.Ops._
      import fscape.graph._
      import fscape.lucre.graph._

      val in = AudioFileIn("file")
      val SR = in.sampleRate
      val sig = in
      Length(sig).poll("in.length")
      val pad = DC(0.0).take(0.5 * SR) ++ sig // avoid stutter in the beginning
      PhysicalOut(pad)
    }

    lazy val gFSc1 = fscape.Graph {
      import fscape.Ops._
      import fscape.graph._

      val n = WhiteNoise()
      val SR = 48000
      val modFreq = Seq[GE](0.1, 0.123).map(_ / SR)
      val freq = SinOsc(modFreq).linExp(-1, 1, 200, 2000)
      val f = LPF(n, freq / SR)
      //      val nw    = 8192 * 4
      //      val norm  = NormalizeWindow(f, nw) * 0.25
      //      val normW = norm * GenWindow.Hann(nw)
      //      val lap   = OverlapAdd(normW, nw, nw / 2)
      //      val sig   = lap
      val sig = f * 100
      Frames(sig.out(0)).poll(Metro(SR), "metro")
      PhysicalOut(sig)
    }

    def any2stringadd: Any = ()

    lazy val gFScBubbles = fscape.Graph {
      import de.sciss.fscape.lucre.graph.Ops._
      import fscape.graph._

      val SR        = 44100
      val fmOff     = "fm-offset" .attr(80)
      val fmODepth  = "fm-depth"  .attr(24)
      val hasVerb   = "reverb"    .attr(1)
      val lfFreq    = "lf-freq"   .attr(0.4)
      val fl        = (LFSaw(lfFreq / SR) * fmODepth + (LFSaw(Seq[GE](8.0 / SR, 7.23 / SR)) * 3 + fmOff)).midiCps
      val sin       = SinOsc(fl / SR) * 0.04
      val sig       = If(hasVerb) Then {
        val echoL = 0.2 * SR
        CombN(sin, echoL, echoL, 4 * SR) // echoing sine wave
      } Else sin
      PhysicalOut(sig)
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

    lazy val gProcBubbles = SynthGraph {
      import synth.proc.graph.Ops._
      import ugen._

      NumOutputBuses.ir.poll(0, "NumOutputBuses")

      val fmOff     = "fm-offset" .kr(80)
      val fmODepth  = "fm-depth"  .kr(24)
      val hasVerb   = "reverb"    .kr(1)
      val lfFreq    = "lf-freq"   .kr(0.4)

      val fl  = LFSaw.ar(lfFreq).mulAdd(fmODepth, LFSaw.ar(Seq(8.0, 7.23)).mulAdd(3, fmOff)).midiCps
      val sin = SinOsc.ar(fl) * 0.04
      val rev = CombN.ar(sin, 0.2, 0.2, 4)
      val sig = Select.ar(hasVerb, Seq(sin, rev))
      //      sig.out(0).poll(1, "sig")
      Out.ar(0, sig)
    }

    lazy val gW0 = swing.Graph {
      import expr.graph._
      import proc.ExImport._
      import swing.graph._

      val rRMS          = Runner("fsc-rms"      )
      val rReplay       = Runner("fsc-replay"   )
      val rBubblesFSc   = Runner("fsc-bubbles"  )
      val rBubblesProc  = Runner("proc-bubbles" )
      val rms           = Var(0.0)
      val state         = Var("")
      val rmsInfo       = Const("RMS is %1.1f dBFS.").format(rms.ampDb)
      val rmsRan        = Var(false)

      val ggFilterSource = ComboBox(
        Seq("WhiteNoise", "Mic Input")
      )
      ggFilterSource.index() = 0

      val ggGenNoise = Button("Render")
      ggGenNoise.clicked ---> Act(
        state.set("..."),
        rRMS.runWith(
          "source" -> ggFilterSource.index(),
          "out" -> rms,
        ),
      )
      ggGenNoise.enabled = {
        val s = rRMS.state
        (s sig_== 0) || (s >= 4)
      }

      val ggReplay = Button("Replay")
      ggReplay.clicked ---> Act(
        PrintLn("run replay"),
        rReplay.run,
      )
      ggReplay.enabled = rmsRan && {
        val s = rReplay.state
        (s sig_== 0) || (s >= 4)
      }

      rRMS.failed ---> PrintLn("FAILED RENDER: " ++ rRMS.messages.mkString(", "))
      rReplay.failed ---> PrintLn("FAILED REPLAY: " ++ rReplay.messages.mkString(", "))

      val slFMOff = Slider()
      slFMOff.min = 40
      slFMOff.max = 120
      slFMOff.value() = 80

      val ifFMDepth = IntField()
      ifFMDepth.min = 0
      ifFMDepth.max = 96
      ifFMDepth.value() = 24
      ifFMDepth.unit = "semitones"

      val dfLFFreq = DoubleField()
      dfLFFreq.min = 0.01
      dfLFFreq.max = 100.0
      dfLFFreq.value() = 0.4
      dfLFFreq.unit = "Hz"

      val cbReverb = CheckBox("Reverb")
      cbReverb.selected() = true

      {
        val idx = ggFilterSource.index()
        idx.changed ---> PrintLn("ComboBox index = " ++ idx.toStr)
      }

      val bang = Bang()

      def mkStartStop(r: Runner): (Widget, Widget) = {
        val ggStart  = Button("Play")
        val ggStop   = Button("Stop")
        ggStart.clicked ---> r.runWith(
          "fm-offset" -> slFMOff  .value(),
          "fm-depth"        -> ifFMDepth.value(),
          "lf-freq"         -> dfLFFreq .value(),
          "reverb"          -> cbReverb .selected(),
        )
        ggStop.clicked ---> r.stop
        ggStart.enabled = {
          val s = r.state
          (s sig_== 0) || (s >= 4)
        }
        ggStop.enabled = !ggStart.enabled
        (ggStart, ggStop)
      }

      val (ggStartBubblesFSc  , ggStopBubblesFSc  ) = mkStartStop(rBubblesFSc )
      val (ggStartBubblesProc , ggStopBubblesProc ) = mkStartStop(rBubblesProc)

      LoadBang() ---> Act(
        PrintLn("Hello from SoundProcesses. Running FScape..."),
        Delay(3.0)(PrintLn("3 seconds have passed. java.vm.name = " ++ Sys.Property("java.vm.name").getOrElse("not defined"))),
        //        fsc.runWith("out" -> res)
      )

      rRMS.done ---> Act(
        PrintLn("FScape completed."),
        state.set(rmsInfo),
        rmsRan.set(true),
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
        Label("Analog Bubbles (Proc):"  ), FlowPanel(ggStartBubblesProc, ggStopBubblesProc ),
        Label("Analog Bubbles (FScape):"), FlowPanel(ggStartBubblesFSc , ggStopBubblesFSc  ),
        Label(" Freq Mod Offset:"       ), slFMOff,
        Label(" Freq Mod Depth:"        ), ifFMDepth,
        Label(" LFO Freq:"              ), dfLFFreq,
      )
      pBubbles.columns = 2
      pBubbles.compactColumns = true

      val progBar  = ProgressBar()
      val prog    = (rRMS.progress * 100).toInt
      progBar.value = prog

      //      prog.changed ---> PrintLn("PROGRESS = " ++ prog.toStr)

      //      val pRMS = FlowPanel(Label("Filtered Noise:"), ggAnalyze, progBar, bang, Label(state))

      val ggDeleteFile = Button("Delete File")
      ggDeleteFile.clicked ---> (Artifact("file"): Ex[URI]).delete

      val pRMS = GridPanel(
        Label("Freq Filter. Source:"),
        FlowPanel(ggFilterSource, ggGenNoise, progBar, ggReplay, ggDeleteFile),
        Empty(), FlowPanel(bang, Label(state)),
      )
      pRMS.columns = 2
      pRMS.compactColumns = true

      //      pRMS.hGap = 10

      //      val checkState = cbReverb.selected()
      //      checkState.changed ---> PrintLn("SELECTED = " ++ checkState.toStr ++ " / " ++ cbReverb.selected().toStr)

      BorderPanel(
        north   = pBubbles,
        center  = FlowPanel(cbReverb), //, ggFilterSource),
        south   = pRMS,
      )
    }

    lazy val gW1 = swing.Graph {
      import swing.graph._

      BorderPanel(
        north = Label("north"),
        south = Label("south"),
        //        center= Separator(),
        center = Label("center"),
        west = Label("west"),
        east = Label("east"),
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

    implicit val undo: UndoManager[T] = UndoManager()

    //    import Workspace.Implicits._

    val (universe, view) = cursor.step { implicit tx =>
      implicit val u: Universe[T] = Universe.dummy[T]

      val fscRMS = FScape[T]()
      fscRMS.graph() = gFScRMS

      val fscReplay = FScape[T]()
      fscReplay.graph() = gFScReplay

      val fscBubbles = FScape[T]()
      fscBubbles.graph() = gFScBubbles

      val procBubbles = Proc[T]()
      procBubbles.graph() = gProcBubbles

      val rootURI = new URI("idb", "/", null)
      val locRMS  = LArtifactLocation.newConst[T](rootURI)
      val artRMS  = LArtifact(locRMS, LArtifact.Child("test.aif"))
      fscRMS    .attr.put("file", artRMS)
      fscReplay .attr.put("file", artRMS)

      //      val w = proc.Control[T]()
      val w = Widget[T]()
      val wAttr = w.attr
      w.graph() = gW
      wAttr.put("fsc-rms"     , fscRMS      )
      wAttr.put("fsc-bubbles" , fscBubbles  )
      wAttr.put("fsc-replay"  , fscReplay   )
      wAttr.put("proc-bubbles", procBubbles )
      wAttr.put("file"        , artRMS      )

      val wH = tx.newHandle(w)
      implicit val ctx: expr.Context[T] = ExprContext(selfH = Some(wH))

      val _view = gW.expand[T]
      _view.initControl()

      u.auralSystem.addClientNow(new AuralSystem.Client {
        override def auralStarted(s: Server)(implicit tx: RT): Unit =
          println("auralStarted")

        override def auralStopped()(implicit tx: RT): Unit =
          println("auralStopped")
      })

      (u, _view)
      //
      //
      //      gEx.expand.initControl()
    }

//    universeOpt = Some(universe)
    Future.successful((universe, view))
  }
}
