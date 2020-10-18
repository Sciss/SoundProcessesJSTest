package de.sciss.synth.proc

import de.sciss.lucre.{InMemory, Workspace}
import de.sciss.lucre.edit.UndoManager
import de.sciss.lucre.expr.{Context, Graph, graph}

object Test {
  def main(args: Array[String]): Unit = {
    run()
  }

  def run(): Unit = {
    type S = InMemory
    type T = InMemory.Txn

    val g = Graph {
      import graph._

      LoadBang() ---> PrintLn("Henlo")
    }

    implicit val system: S = InMemory()
    implicit val undo: UndoManager[T] = UndoManager()

    import Workspace.Implicits._

    implicit val ctx: Context[T] = Context()

    system.step { implicit tx =>
      g.expand.initControl()
    }
  }
}