package de.sciss.proc

import de.sciss.lucre.edit.UndoManager
import de.sciss.lucre.swing.View
import de.sciss.lucre.{Cursor, expr}
import de.sciss.proc.Implicits.FolderOps
import de.sciss.proc.Test.T
import org.scalajs.dom.raw.XMLHttpRequest

import scala.concurrent.{Future, Promise}
import scala.scalajs.js.typedarray.{ArrayBuffer, Int8Array}
import scala.util.control.NonFatal

object LoadWorkspace {
  def apply(url: String = "workspace.mllt.bin"): Future[(Universe[T], View[T])] = {
    val oReq  = new XMLHttpRequest
    oReq.open(method = "GET", url = url, async = true)
    oReq.responseType = "arraybuffer"
    val res = Promise[(Universe[T], View[T])]()

    oReq.onload = { _ =>
      oReq.response match {
        case ab: ArrayBuffer =>
          try {
            val bytes = new Int8Array(ab).toArray
            val ws    = Workspace.Blob.fromByteArray(bytes)
            println("Workspace meta data:")
            ws.meta.foreach(println)
            implicit val cursor: Cursor[T] = ws.cursor
            implicit val undo: UndoManager[T] = UndoManager()
            val resOpt = cursor.step { implicit tx =>
              val fRoot = ws.root
//              fRoot.iterator.foreach { child =>
//                println(s"CHILD: ${child.name}")
//              }
              fRoot.$[Widget]("start").map { w =>
                implicit val u: Universe[T] = Universe.dummy[T]
                val wH = tx.newHandle(w)
                implicit val ctx: expr.Context[T] = ExprContext(selfH = Some(wH))
                val gW    = w.graph().value
                val _view = gW.expand[T]
                _view.initControl()
                (u, _view)
              }
            }
            val resVal = resOpt.getOrElse(sys.error("No start element found"))
            res.success(resVal)

          } catch {
            case NonFatal(ex) => res.failure(ex)
          }

        case other =>
          res.failure(new Exception(s"Expected an ArrayBuffer but got $other"))
      }
    }
    oReq.onerror = { _ =>
      res.failure(new Exception(s"XMLHttpRequest failed for '$url'"))
    }

    oReq.send(null)

    res.future
  }
}
