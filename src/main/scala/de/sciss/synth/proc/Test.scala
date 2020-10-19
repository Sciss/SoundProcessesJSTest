package de.sciss.synth.proc

import de.sciss.fscape
import de.sciss.lucre.edit.UndoManager
import de.sciss.lucre.expr
import de.sciss.lucre.synth.InMemory
import de.sciss.synth.proc

object Test {
  def main(args: Array[String]): Unit = {
    run()
  }

  def run(): Unit = {
    type S = InMemory
    type T = InMemory.Txn

    val gFSc = fscape.Graph {
      import fscape.graph._
      import fscape.lucre.graph._

      val l   = 1000
      val n   = WhiteNoise().take(l)
      val f   = LPF(n, 200.0/44100.0)
      val rms = (RunningSum(f.squared) / l).sqrt
      MkDouble("out", rms)
    }

    val gEx = expr.Graph {
      import expr.graph._
      import expr.ExImport._

      val fsc = Runner("fsc")
      val res = Var(0.0)

      LoadBang() ---> Act(
        PrintLn("Hello from SoundProcesses. Running FScape..."),
        Delay(3.0)(PrintLn("user.name = " ++ Sys.Property("user.name").getOrElse("not defined"))),
        fsc.runWith("out" -> res)
      )

      fsc.done ---> Act(
        PrintLn("FScape completed. RMS is " ++ res.toStr)
      )
    }

    implicit val system: S = InMemory()
    implicit val undo: UndoManager[T] = UndoManager()

//    import Workspace.Implicits._

    system.step { implicit tx =>
      implicit val u: Universe[T] = Universe.dummy[T]

      val fsc = fscape.lucre.FScape[T]()
      fsc.graph() = gFSc

      val ex = proc.Control[T]()
      ex.graph() = gEx
      ex.attr.put("fsc", fsc)

      val r = proc.Runner(ex)
      r.run()
//
//      implicit val ctx: expr.Context[T] = ExprContext()
//
//      gEx.expand.initControl()
    }
  }
}