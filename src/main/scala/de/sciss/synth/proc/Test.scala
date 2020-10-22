package de.sciss.synth.proc

import de.sciss.fscape
import de.sciss.fscape.GE
import de.sciss.fscape.lucre.FScape
import de.sciss.lucre.edit.UndoManager
import de.sciss.lucre.expr
import de.sciss.lucre.synth.InMemory
import de.sciss.synth.proc

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("Test")
object Test {
  def main(args: Array[String]): Unit = {
    println("Test initialized.")
  }

  @JSExport
  def run(): Unit = {
    type S = InMemory
    type T = InMemory.Txn

    FScape.init()

    val cfg = FScape.Config()
    cfg.blockSize = 4096
    FScape.defaultConfig = cfg

    //    val gFSc = fscape.Graph {
//      import fscape.graph._
//      import fscape.lucre.graph._
//
//      val m   = 1000
//      val n   = WhiteNoise().take(m)
//      val f   = LPF(n, 200.0/44100.0)
//      val rms = (RunningSum(f.squared).last / m).sqrt
//      MkDouble("out", rms)
//    }

    lazy val gFSc0 = fscape.Graph {
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

    lazy val gFSc = fscape.Graph {
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

    val gEx = expr.Graph {
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

    implicit val system: S = InMemory()
    implicit val undo: UndoManager[T] = UndoManager()

//    import Workspace.Implicits._

    system.step { implicit tx =>
      implicit val u: Universe[T] = Universe.dummy[T]

      val fsc = FScape[T]()
      fsc.graph() = gFSc

      val ex = proc.Control[T]()
      ex.graph() = gEx
      ex.attr.put("fsc", fsc)

      val r = proc.Runner(ex)
//      println(r)
      r.run()
      r.reactNow { implicit tx => state =>
        println(s"STATE = $state")
      }
//
//      implicit val ctx: expr.Context[T] = ExprContext()
//
//      gEx.expand.initControl()
    }

    println("End of main.")
  }
}