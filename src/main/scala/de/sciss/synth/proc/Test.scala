package de.sciss.synth.proc

import de.sciss.lucre.edit.UndoManager
import de.sciss.lucre.expr.{Context, Graph, graph}
import de.sciss.lucre.synth.InMemory

object Test {
  def main(args: Array[String]): Unit = {
    run()
  }

  def run(): Unit = {
    type S = InMemory
    type T = InMemory.Txn

    val g = Graph {
      import graph._

      LoadBang() ---> Act(
        PrintLn("Henlo"),
        Delay(3.0)(PrintLn(Sys.Property("user.name").getOrElse("unknown creature"))),
      )
    }

    implicit val system: S = InMemory()
    implicit val undo: UndoManager[T] = UndoManager()

//    import Workspace.Implicits._

    system.step { implicit tx =>
      implicit val u  : Universe[T] = Universe.dummy[T]
      implicit val ctx: Context [T] = ExprContext()

      g.expand.initControl()
    }
  }
}