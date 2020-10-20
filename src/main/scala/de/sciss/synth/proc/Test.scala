package de.sciss.synth.proc

import de.sciss.fscape
import de.sciss.fscape.GE
import de.sciss.lucre.edit.UndoManager
import de.sciss.lucre.expr
import de.sciss.lucre.synth.InMemory
import de.sciss.synth.proc

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("Test")
object Test {
  def main(args: Array[String]): Unit = {
    println("Test initalized.")
  }

  @JSExport
  def run(): Unit = {
    type S = InMemory
    type T = InMemory.Txn

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

    val gFSc = fscape.Graph {
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

    fscape.lucre.FScape.init()

    system.step { implicit tx =>
      implicit val u: Universe[T] = Universe.dummy[T]

      val fsc = fscape.lucre.FScape[T]()
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